package com.example.rag.test;

import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.BytesRef;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 解析 /Users/songzhiquan1/IdeaProjects/rag/data/index9/_9_0.tim 文件内容
 * .tim 文件是 Lucene 的 Term Dictionary 文件，存储了所有 term 的字典数据。
 *
 * 解析方式：
 * 1. 底层：解析 Codec Header / Footer 及原始数据的 Hex Dump
 * 2. 高层：通过 DirectoryReader API 读取该 segment 中的所有 term 信息
 *    （包括字段名、term 文本、docFreq、totalTermFreq 等）
 *
 * 将解析结果保存到 data/output/9/_9_0_tim_content.txt
 */
public class Tim9Reader {

    private static final String INDEX_PATH = "/Users/songzhiquan1/IdeaProjects/rag/data/index9";
    private static final String FILE_NAME = "_9_0.tim";
    private static final String OUTPUT_PATH = "/Users/songzhiquan1/IdeaProjects/rag/data/output/9/_9_0_tim_content.txt";

    public static void main(String[] args) {
        try {
            // 确保输出目录存在
            Path outputDir = Paths.get(OUTPUT_PATH).getParent();
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_PATH))) {

                writer.println("==========================================================");
                writer.println(" Lucene _9_0.tim 文件解析 (Term Dictionary)");
                writer.println("==========================================================");
                writer.println("文件路径: " + INDEX_PATH + "/" + FILE_NAME);
                writer.println();

                // ====== Part 1: 底层解析 Codec Header 和 Footer ======
                parseRawFile(writer);

                writer.println();

                // ====== Part 2: 通过 Lucene API 读取 Term Dictionary 内容 ======
                parseWithLuceneAPI(writer);

                writer.println();
                writer.println("==========================================================");
                writer.println(" 解析完成!");
                writer.println("==========================================================");

                System.out.println("解析完成！结果已保存到: " + OUTPUT_PATH);
            }
        } catch (Exception e) {
            System.err.println("解析失败:");
            e.printStackTrace();
        }
    }

    /**
     * Part 1: 底层解析 .tim 文件的 Codec Header、Footer 和原始 Hex Dump
     */
    private static void parseRawFile(PrintWriter writer) throws Exception {
        try (Directory directory = FSDirectory.open(Paths.get(INDEX_PATH));
             IndexInput input = directory.openInput(FILE_NAME, IOContext.READONCE)) {

            long fileLength = input.length();
            writer.println("--- 文件基本信息 ---");
            writer.println("文件长度: " + fileLength + " bytes");
            writer.println();

            // 1. 解析 Codec Header
            writer.println("--- Codec Header ---");
            int magic = input.readInt();
            writer.printf("Magic Number: 0x%08X%n", magic);
            if (magic == CodecUtil.CODEC_MAGIC) {
                writer.println("  (有效 CodecUtil.CODEC_MAGIC)");
            } else {
                writer.println("  (注意: 字节序可能不同，实际读取值)");
            }

            String codecName = input.readString();
            writer.println("Codec Name: " + codecName);

            int version = input.readInt();
            writer.println("Version: " + version);

            // Segment ID (16 bytes)
            byte[] segmentId = new byte[16];
            input.readBytes(segmentId, 0, 16);
            writer.print("Segment ID: ");
            StringBuilder sb = new StringBuilder();
            for (byte b : segmentId) {
                sb.append(String.format("%02X", b));
            }
            writer.println(sb.toString());

            // Suffix
            String suffix = input.readString();
            writer.println("Suffix: \"" + suffix + "\"");

            long headerEnd = input.getFilePointer();
            writer.println("Header 结束位置 (offset): " + headerEnd);
            writer.println();

            // Footer 占 16 bytes
            long footerStart = fileLength - 16;

            // 2. 解析 Codec Footer
            writer.println("--- Codec Footer ---");
            input.seek(footerStart);
            int footerMagic = input.readInt();
            writer.printf("Footer Magic: 0x%08X%n", footerMagic);

            int algorithmID = input.readInt();
            writer.println("Algorithm ID: " + algorithmID);

            long checksum = input.readLong();
            writer.printf("Checksum: %d (0x%016X)%n", checksum, checksum);
            writer.println();

            // 3. 数据区域 Hex Dump
            writer.println("--- 数据区域 Hex Dump ---");
            writer.println("数据区域: offset " + headerEnd + " ~ " + (footerStart - 1));
            writer.println("数据区域长度: " + (footerStart - headerEnd) + " bytes");
            writer.println();

            input.seek(headerEnd);
            int bytesPerLine = 16;
            long pos = headerEnd;
            while (pos < footerStart) {
                writer.printf("Offset %4d: ", pos);
                StringBuilder hexPart = new StringBuilder();
                StringBuilder asciiPart = new StringBuilder();
                int count = (int) Math.min(bytesPerLine, footerStart - pos);
                for (int i = 0; i < count; i++) {
                    byte b = input.readByte();
                    hexPart.append(String.format("%02X ", b));
                    asciiPart.append((b >= 32 && b < 127) ? (char) b : '.');
                }
                writer.printf("%-48s | %s%n", hexPart.toString(), asciiPart.toString());
                pos += count;
            }
        }
    }

    /**
     * Part 2: 通过 Lucene DirectoryReader API 正确解析 Term Dictionary 的内容
     * 这是读取 .tim 文件语义数据的正确方式
     */
    private static void parseWithLuceneAPI(PrintWriter writer) throws Exception {
        writer.println("==========================================================");
        writer.println(" 通过 Lucene API 解析 Term Dictionary 语义内容");
        writer.println(" (对应 _9_0.tim 文件中存储的 term 数据)");
        writer.println("==========================================================");
        writer.println();

        try (FSDirectory dir = FSDirectory.open(Paths.get(INDEX_PATH));
             IndexReader reader = DirectoryReader.open(dir)) {

            int totalTerms = 0;

            for (LeafReaderContext leafContext : reader.leaves()) {
                LeafReader leafReader = leafContext.reader();
                SegmentReader segReader = (SegmentReader) leafReader;
                String segName = segReader.getSegmentInfo().info.name;

                // 只处理 segment _9 的数据（对应 _9_0.tim）
                if (!"_9".equals(segName)) {
                    continue;
                }

                writer.println("[Segment: " + segName + "]");
                writer.println("  最大文档数: " + leafReader.maxDoc());
                writer.println("  有效文档数: " + leafReader.numDocs());
                writer.println();

                // 获取所有字段
                FieldInfos fieldInfos = leafReader.getFieldInfos();

                for (FieldInfo fieldInfo : fieldInfos) {
                    String fieldName = fieldInfo.name;
                    IndexOptions indexOptions = fieldInfo.getIndexOptions();

                    if (indexOptions == IndexOptions.NONE) {
                        continue;
                    }

                    Terms terms = leafReader.terms(fieldName);
                    if (terms == null) {
                        continue;
                    }

                    writer.println("----------------------------------------------------------");
                    writer.println("[Field] " + fieldName);
                    writer.println("  IndexOptions: " + indexOptions);
                    writer.println("  DocCount: " + terms.getDocCount());
                    writer.println("  SumTotalTermFreq: " + terms.getSumTotalTermFreq());
                    writer.println("  SumDocFreq: " + terms.getSumDocFreq());
                    writer.println("  TermCount (size): " + terms.size());
                    writer.println("  HasFreqs: " + terms.hasFreqs());
                    writer.println("  HasPositions: " + terms.hasPositions());
                    writer.println("  HasOffsets: " + terms.hasOffsets());
                    writer.println("  HasPayloads: " + terms.hasPayloads());
                    writer.println();

                    // 遍历所有 term
                    TermsEnum termsEnum = terms.iterator();
                    BytesRef termBytes;
                    int fieldTermCount = 0;

                    while ((termBytes = termsEnum.next()) != null) {
                        fieldTermCount++;
                        totalTerms++;

                        String termText = termBytes.utf8ToString();
                        int docFreq = termsEnum.docFreq();
                        long totalTermFreq = termsEnum.totalTermFreq();

                        writer.printf("  [Term #%d] \"%s\"%n", fieldTermCount, termText);
                        writer.printf("    Bytes (hex): %s%n", bytesRefToHex(termBytes));
                        writer.printf("    DocFreq: %d%n", docFreq);
                        writer.printf("    TotalTermFreq: %d%n", totalTermFreq);

                        // 读取 postings 信息
                        PostingsEnum postings = termsEnum.postings(null, PostingsEnum.FREQS);
                        if (postings != null) {
                            StringBuilder postingsInfo = new StringBuilder();
                            int doc;
                            while ((doc = postings.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                                if (postingsInfo.length() > 0) postingsInfo.append(", ");
                                postingsInfo.append("doc=" + doc + "/freq=" + postings.freq());
                            }
                            writer.println("    Postings: [" + postingsInfo + "]");
                        }
                        writer.println();
                    }

                    writer.println("  该字段 Term 总数: " + fieldTermCount);
                    writer.println();
                }
            }

            writer.println("----------------------------------------------------------");
            writer.println("所有字段 Term 总数: " + totalTerms);
        }
    }

    /**
     * BytesRef 转 hex 字符串
     */
    private static String bytesRefToHex(BytesRef ref) {
        StringBuilder sb = new StringBuilder();
        for (int i = ref.offset; i < ref.offset + ref.length; i++) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(String.format("%02X", ref.bytes[i] & 0xFF));
        }
        return sb.toString();
    }
}