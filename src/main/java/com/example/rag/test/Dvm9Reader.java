package com.example.rag.test;

import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 解析 /Users/songzhiquan1/IdeaProjects/rag/data/index9/_9_Lucene90_0.dvm 文件内容
 * .dvm 文件是 Lucene90 DocValues 的元数据文件 (DocValues Metadata)。
 * 它存储了 DocValues 字段的元信息，包括字段编号、DocValues类型、
 * 数据偏移量和长度等。
 *
 * 解析方式：
 * 1. 底层：解析 Codec Header / Footer 及原始数据的 Hex Dump
 * 2. 高层：通过 Lucene API 读取该 segment 中 DocValues 字段的信息
 *
 * 将解析结果保存到 data/output/9/_9_Lucene90_0_dvm_content.txt
 */
public class Dvm9Reader {

    private static final String INDEX_PATH = "/Users/songzhiquan1/IdeaProjects/rag/data/index9";
    private static final String FILE_NAME = "_9_Lucene90_0.dvm";
    private static final String OUTPUT_PATH = "/Users/songzhiquan1/IdeaProjects/rag/data/output/9/_9_Lucene90_0_dvm_content.txt";

    public static void main(String[] args) {
        try {
            // 确保输出目录存在
            Path outputDir = Paths.get(OUTPUT_PATH).getParent();
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_PATH))) {

                writer.println("==========================================================");
                writer.println(" Lucene _9_Lucene90_0.dvm 文件解析 (DocValues Metadata)");
                writer.println(" 编解码器: Lucene90DocValuesFormat");
                writer.println("==========================================================");
                writer.println("文件路径: " + INDEX_PATH + "/" + FILE_NAME);
                writer.println();

                // ====== Part 1: 底层解析 Codec Header 和 Footer ======
                parseRawFile(writer);

                writer.println();

                // ====== Part 2: 通过 Lucene API 读取 DocValues 信息 ======
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
     * Part 1: 底层解析 .dvm 文件的 Codec Header、Footer 和原始 Hex Dump
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
                writer.println("  (注意: 不是标准 CODEC_MAGIC)");
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

            // 2. 数据区域 - 尝试以 VInt 方式解析
            writer.println("--- 数据区域 (VInt 解析) ---");
            writer.println("数据区域: offset " + headerEnd + " ~ " + (footerStart - 1));
            writer.println("数据区域长度: " + (footerStart - headerEnd) + " bytes");
            writer.println();

            long dataStart = headerEnd;
            input.seek(dataStart);
            int index = 0;
            while (input.getFilePointer() < footerStart) {
                long offset = input.getFilePointer();
                try {
                    int val = input.readVInt();
                    writer.printf("[%3d] Offset: %4d | VInt值: %d%n", index, offset, val);
                    index++;
                } catch (Exception e) {
                    writer.printf("[%3d] Offset: %4d | 读取VInt失败: %s%n", index, offset, e.getMessage());
                    input.seek(offset + 1);
                    index++;
                }
            }
            writer.println();

            // 3. 原始字节 Hex Dump
            writer.println("--- 原始字节 (Hex Dump) ---");
            input.seek(dataStart);
            int bytesPerLine = 16;
            long pos = dataStart;
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
            writer.println();

            // 4. 解析 Codec Footer
            writer.println("--- Codec Footer ---");
            input.seek(footerStart);
            int footerMagic = input.readInt();
            writer.printf("Footer Magic: 0x%08X%n", footerMagic);

            int algorithmID = input.readInt();
            writer.println("Algorithm ID: " + algorithmID);

            long checksum = input.readLong();
            writer.printf("Checksum: %d (0x%016X)%n", checksum, checksum);
        }
    }

    /**
     * Part 2: 通过 Lucene DirectoryReader API 解析 DocValues 语义内容
     * .dvm 文件存储了 DocValues 的元数据，通过 API 可以读取实际的 DocValues 数据
     */
    private static void parseWithLuceneAPI(PrintWriter writer) throws Exception {
        writer.println("==========================================================");
        writer.println(" 通过 Lucene API 解析 DocValues 语义内容");
        writer.println(" (对应 _9_Lucene90_0.dvm 文件中存储的 DocValues 元数据)");
        writer.println("==========================================================");
        writer.println();

        try (FSDirectory dir = FSDirectory.open(Paths.get(INDEX_PATH));
             IndexReader reader = DirectoryReader.open(dir)) {

            for (LeafReaderContext leafContext : reader.leaves()) {
                LeafReader leafReader = leafContext.reader();
                SegmentReader segReader = (SegmentReader) leafReader;
                String segName = segReader.getSegmentInfo().info.name;

                // 只处理 segment _9 的数据
                if (!"_9".equals(segName)) {
                    continue;
                }

                writer.println("[Segment: " + segName + "]");
                writer.println("  最大文档数: " + leafReader.maxDoc());
                writer.println("  有效文档数: " + leafReader.numDocs());
                writer.println();

                // 获取所有字段信息
                FieldInfos fieldInfos = leafReader.getFieldInfos();

                for (FieldInfo fieldInfo : fieldInfos) {
                    String fieldName = fieldInfo.name;
                    DocValuesType dvType = fieldInfo.getDocValuesType();

                    if (dvType == DocValuesType.NONE) {
                        continue;
                    }

                    writer.println("----------------------------------------------------------");
                    writer.println("[DocValues Field] " + fieldName);
                    writer.println("  Field Number: " + fieldInfo.number);
                    writer.println("  DocValues Type: " + dvType);
                    writer.println();

                    switch (dvType) {
                        case NUMERIC:
                            parseNumericDocValues(leafReader, fieldName, writer);
                            break;
                        case BINARY:
                            parseBinaryDocValues(leafReader, fieldName, writer);
                            break;
                        case SORTED:
                            parseSortedDocValues(leafReader, fieldName, writer);
                            break;
                        case SORTED_NUMERIC:
                            parseSortedNumericDocValues(leafReader, fieldName, writer);
                            break;
                        case SORTED_SET:
                            parseSortedSetDocValues(leafReader, fieldName, writer);
                            break;
                        default:
                            writer.println("  (未知 DocValues 类型)");
                    }
                    writer.println();
                }
            }
        }
    }

    /**
     * 解析 NUMERIC 类型的 DocValues
     */
    private static void parseNumericDocValues(LeafReader reader, String field, PrintWriter writer) throws Exception {
        NumericDocValues ndv = reader.getNumericDocValues(field);
        if (ndv == null) {
            writer.println("  (无数据)");
            return;
        }

        writer.println("  --- Numeric DocValues ---");
        int count = 0;
        while (ndv.nextDoc() != NumericDocValues.NO_MORE_DOCS) {
            int docId = ndv.docID();
            long value = ndv.longValue();
            writer.printf("    doc=%d, value=%d%n", docId, value);
            count++;
            if (count >= 100) {
                writer.println("    ... (超过100条，截断显示)");
                break;
            }
        }
        writer.println("  总计: " + count + " 条记录");
    }

    /**
     * 解析 BINARY 类型的 DocValues
     */
    private static void parseBinaryDocValues(LeafReader reader, String field, PrintWriter writer) throws Exception {
        BinaryDocValues bdv = reader.getBinaryDocValues(field);
        if (bdv == null) {
            writer.println("  (无数据)");
            return;
        }

        writer.println("  --- Binary DocValues ---");
        int count = 0;
        while (bdv.nextDoc() != BinaryDocValues.NO_MORE_DOCS) {
            int docId = bdv.docID();
            byte[] bytes = bdv.binaryValue().bytes;
            int offset = bdv.binaryValue().offset;
            int length = bdv.binaryValue().length;

            String text = new String(bytes, offset, length, java.nio.charset.StandardCharsets.UTF_8);
            writer.printf("    doc=%d, length=%d, value=\"%s\"%n", docId, length,
                    text.length() > 100 ? text.substring(0, 100) + "..." : text);
            count++;
            if (count >= 100) {
                writer.println("    ... (超过100条，截断显示)");
                break;
            }
        }
        writer.println("  总计: " + count + " 条记录");
    }

    /**
     * 解析 SORTED 类型的 DocValues
     */
    private static void parseSortedDocValues(LeafReader reader, String field, PrintWriter writer) throws Exception {
        SortedDocValues sdv = reader.getSortedDocValues(field);
        if (sdv == null) {
            writer.println("  (无数据)");
            return;
        }

        int valueCount = sdv.getValueCount();
        writer.println("  --- Sorted DocValues ---");
        writer.println("  唯一值数量: " + valueCount);
        writer.println();

        // 输出所有唯一值(ord -> value)
        writer.println("  [Ord -> Value 映射]");
        for (int ord = 0; ord < Math.min(valueCount, 50); ord++) {
            byte[] bytes = sdv.lookupOrd(ord).bytes;
            int offset = sdv.lookupOrd(ord).offset;
            int length = sdv.lookupOrd(ord).length;
            String val = new String(bytes, offset, length, java.nio.charset.StandardCharsets.UTF_8);
            writer.printf("    ord=%d -> \"%s\"%n", ord, val.length() > 80 ? val.substring(0, 80) + "..." : val);
        }
        if (valueCount > 50) {
            writer.println("    ... (超过50个唯一值，截断显示)");
        }
        writer.println();

        // 输出 doc -> ord 映射
        writer.println("  [Doc -> Ord 映射]");
        int count = 0;
        while (sdv.nextDoc() != SortedDocValues.NO_MORE_DOCS) {
            int docId = sdv.docID();
            int ord = sdv.ordValue();
            byte[] bytes = sdv.lookupOrd(ord).bytes;
            int offset = sdv.lookupOrd(ord).offset;
            int length = sdv.lookupOrd(ord).length;
            String val = new String(bytes, offset, length, java.nio.charset.StandardCharsets.UTF_8);
            writer.printf("    doc=%d, ord=%d, value=\"%s\"%n", docId, ord,
                    val.length() > 60 ? val.substring(0, 60) + "..." : val);
            count++;
            if (count >= 100) {
                writer.println("    ... (超过100条，截断显示)");
                break;
            }
        }
        writer.println("  总计: " + count + " 条记录");
    }

    /**
     * 解析 SORTED_NUMERIC 类型的 DocValues
     */
    private static void parseSortedNumericDocValues(LeafReader reader, String field, PrintWriter writer) throws Exception {
        SortedNumericDocValues sndv = reader.getSortedNumericDocValues(field);
        if (sndv == null) {
            writer.println("  (无数据)");
            return;
        }

        writer.println("  --- Sorted Numeric DocValues ---");
        int count = 0;
        while (sndv.nextDoc() != SortedNumericDocValues.NO_MORE_DOCS) {
            int docId = sndv.docID();
            int valueCount = sndv.docValueCount();
            StringBuilder values = new StringBuilder();
            for (int i = 0; i < valueCount; i++) {
                if (i > 0) values.append(", ");
                values.append(sndv.nextValue());
            }
            writer.printf("    doc=%d, count=%d, values=[%s]%n", docId, valueCount, values);
            count++;
            if (count >= 100) {
                writer.println("    ... (超过100条，截断显示)");
                break;
            }
        }
        writer.println("  总计: " + count + " 条记录");
    }

    /**
     * 解析 SORTED_SET 类型的 DocValues
     */
    private static void parseSortedSetDocValues(LeafReader reader, String field, PrintWriter writer) throws Exception {
        SortedSetDocValues ssdv = reader.getSortedSetDocValues(field);
        if (ssdv == null) {
            writer.println("  (无数据)");
            return;
        }

        long valueCount = ssdv.getValueCount();
        writer.println("  --- Sorted Set DocValues ---");
        writer.println("  唯一值数量: " + valueCount);
        writer.println();

        // 输出所有唯一值
        writer.println("  [Ord -> Value 映射]");
        for (long ord = 0; ord < Math.min(valueCount, 50); ord++) {
            byte[] bytes = ssdv.lookupOrd(ord).bytes;
            int offset = ssdv.lookupOrd(ord).offset;
            int length = ssdv.lookupOrd(ord).length;
            String val = new String(bytes, offset, length, java.nio.charset.StandardCharsets.UTF_8);
            writer.printf("    ord=%d -> \"%s\"%n", ord, val.length() > 80 ? val.substring(0, 80) + "..." : val);
        }
        if (valueCount > 50) {
            writer.println("    ... (超过50个唯一值，截断显示)");
        }
        writer.println();

        // 输出 doc -> ords 映射
        writer.println("  [Doc -> Ords 映射]");
        int count = 0;
        while (ssdv.nextDoc() != SortedSetDocValues.NO_MORE_DOCS) {
            int docId = ssdv.docID();
            StringBuilder ords = new StringBuilder();
            long ord;
            while ((ord = ssdv.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS) {
                if (ords.length() > 0) ords.append(", ");
                byte[] bytes = ssdv.lookupOrd(ord).bytes;
                int offset = ssdv.lookupOrd(ord).offset;
                int length = ssdv.lookupOrd(ord).length;
                String val = new String(bytes, offset, length, java.nio.charset.StandardCharsets.UTF_8);
                ords.append("ord=" + ord + "/\"" + (val.length() > 30 ? val.substring(0, 30) + "..." : val) + "\"");
            }
            writer.printf("    doc=%d -> [%s]%n", docId, ords);
            count++;
            if (count >= 100) {
                writer.println("    ... (超过100条，截断显示)");
                break;
            }
        }
        writer.println("  总计: " + count + " 条记录");
    }
}