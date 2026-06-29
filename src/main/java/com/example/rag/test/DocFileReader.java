//package com.example.rag.test;
//
//import org.apache.lucene.index.*;
//import org.apache.lucene.store.FSDirectory;
//import org.apache.lucene.util.BytesRef;
//import org.elasticsearch.index.mapper.Uid;
//
//import java.io.BufferedWriter;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//
///**
// * 解析 Lucene 索引中 _9_0.doc 文件的内容（即 Postings 倒排索引数据）
// * _9_0.doc 存储了每个 term 对应的文档ID列表和词频(term frequency)信息。
// * 通过 DirectoryReader 打开索引目录，遍历所有字段的所有 term，
// * 读取每个 term 的 postings（docId + freq），输出到文件中。
// */
//public class DocFileReader {
//
//    private static final String INDEX_PATH = "/Users/songzhiquan1/IdeaProjects/rag/data/index9";
//    private static final String OUTPUT_PATH = "/Users/songzhiquan1/IdeaProjects/rag/data/output/9/_9_0_doc_content.txt";
//
//    public static void main(String[] args) {
//        try {
//            // 确保输出目录存在
//            Path outputDir = Paths.get(OUTPUT_PATH).getParent();
//            if (!Files.exists(outputDir)) {
//                Files.createDirectories(outputDir);
//            }
//
//            try (FSDirectory dir = FSDirectory.open(Paths.get(INDEX_PATH));
//                 IndexReader reader = DirectoryReader.open(dir);
//                 BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_PATH))) {
//
//                writer.write("==========================================================");
//                writer.newLine();
//                writer.write(" 解析索引目录: " + INDEX_PATH);
//                writer.newLine();
//                writer.write(" 目标: _9_0.doc (Postings - 倒排索引文档频率数据)");
//                writer.newLine();
//                writer.write(" 说明: .doc文件存储了每个term对应的文档ID和词频信息");
//                writer.newLine();
//                writer.write("==========================================================");
//                writer.newLine();
//                writer.newLine();
//
//                int totalTerms = 0;
//                int totalPostings = 0;
//
//                // 遍历所有段 (segments)
//                for (LeafReaderContext leafContext : reader.leaves()) {
//                    LeafReader leafReader = leafContext.reader();
//                    String segmentInfo = leafReader.toString();
//
//                    writer.write("[Segment: " + segmentInfo + "]");
//                    writer.newLine();
//                    writer.write("  段内最大文档数: " + leafReader.maxDoc());
//                    writer.newLine();
//                    writer.write("  段内有效文档数: " + leafReader.numDocs());
//                    writer.newLine();
//                    writer.newLine();
//
//                    // 获取所有字段信息
//                    FieldInfos fieldInfos = leafReader.getFieldInfos();
//
//                    for (FieldInfo fieldInfo : fieldInfos) {
//                        String fieldName = fieldInfo.name;
//                        IndexOptions indexOptions = fieldInfo.getIndexOptions();
//
//                        // 只处理被索引的字段（有倒排索引的字段）
//                        if (indexOptions == IndexOptions.NONE) {
//                            continue;
//                        }
//
//                        Terms terms = leafReader.terms(fieldName);
//                        if (terms == null) {
//                            continue;
//                        }
//
//                        writer.write("----------------------------------------------------------");
//                        writer.newLine();
//                        writer.write("[Field] " + fieldName);
//                        writer.newLine();
//                        writer.write("  IndexOptions: " + indexOptions);
//                        writer.newLine();
//                        writer.write("  DocCount: " + terms.getDocCount());
//                        writer.newLine();
//                        writer.write("  SumTotalTermFreq: " + terms.getSumTotalTermFreq());
//                        writer.newLine();
//                        writer.write("  SumDocFreq: " + terms.getSumDocFreq());
//                        writer.newLine();
//                        writer.write("  HasFreqs: " + terms.hasFreqs());
//                        writer.newLine();
//                        writer.write("  HasPositions: " + terms.hasPositions());
//                        writer.newLine();
//                        writer.write("  HasOffsets: " + terms.hasOffsets());
//                        writer.newLine();
//                        writer.newLine();
//
//                        // 遍历该字段的所有 term
//                        TermsEnum termsEnum = terms.iterator();
//                        BytesRef termBytes;
//
//                        while ((termBytes = termsEnum.next()) != null) {
//                            String termText = decodeTerm(fieldName, termBytes);
//                            int docFreq = termsEnum.docFreq();
//                            long totalTermFreq = termsEnum.totalTermFreq();
//
//                            writer.write("  [Term] \"" + termText + "\"");
//                            writer.newLine();
//                            writer.write("    DocFreq(出现在多少个文档中): " + docFreq);
//                            writer.newLine();
//                            writer.write("    TotalTermFreq(总出现次数): " + totalTermFreq);
//                            writer.newLine();
//
//                            // 读取该 term 的 postings 列表（docId + freq）
//                            PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.FREQS);
//                            if (postingsEnum != null) {
//                                writer.write("    Postings(文档ID -> 词频):");
//                                writer.newLine();
//
//                                int doc;
//                                int postingCount = 0;
//                                while ((doc = postingsEnum.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
//                                    int freq = postingsEnum.freq();
//                                    writer.write("      docId=" + doc + ", freq=" + freq);
//                                    writer.newLine();
//                                    postingCount++;
//                                    totalPostings++;
//                                }
//
//                                // 如果支持位置信息，再额外输出位置
//                                if (indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0) {
//                                    PostingsEnum posEnum = termsEnum.postings(null, PostingsEnum.ALL);
//                                    if (posEnum != null) {
//                                        writer.write("    Positions(文档ID -> 位置列表):");
//                                        writer.newLine();
//                                        int posDoc;
//                                        while ((posDoc = posEnum.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
//                                            int posFreq = posEnum.freq();
//                                            StringBuilder positions = new StringBuilder();
//                                            for (int i = 0; i < posFreq; i++) {
//                                                int pos = posEnum.nextPosition();
//                                                if (i > 0) positions.append(", ");
//                                               positions.append(pos);
//                                            }
//                                            writer.write("      docId=" + posDoc + ", positions=[" + positions + "]");
//                                            writer.newLine();
//                                        }
//                                    }
//                                }
//                            }
//
//                            writer.newLine();
//                            totalTerms++;
//                        }
//                    }
//                }
//
//                writer.newLine();
//                writer.write("==========================================================");
//                writer.newLine();
//                writer.write(" 解析完成!");
//                writer.newLine();
//                writer.write(" 共解析 " + totalTerms + " 个 term");
//                writer.newLine();
//                writer.write(" 共解析 " + totalPostings + " 条 postings 记录");
//                writer.newLine();
//                writer.write("==========================================================");
//                writer.newLine();
//
//                System.out.println("解析完成!");
//                System.out.println("共解析 " + totalTerms + " 个 term, " + totalPostings + " 条 postings 记录");
//                System.out.println("输出文件: " + OUTPUT_PATH);
//            }
//
//        } catch (IOException e) {
//            System.err.println("解析失败: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 解码 term 的二进制内容为可读字符串。
//     * - _id 字段：使用 ES 的 Uid.decodeId 解码
//     * - 其他字段：如果内容可打印则直接转字符串，否则展示为十六进制
//     */
//    private static String decodeTerm(String fieldName, BytesRef termBytes) {
//        if ("_id".equals(fieldName)) {
//            try {
//                return Uid.decodeId(termBytes.bytes, termBytes.offset, termBytes.length);
//            } catch (Exception e) {
//                return bytesToHex(termBytes);
//            }
//        }
//
//        // 尝试作为 UTF-8 字符串，检查是否可打印
//        String text = termBytes.utf8ToString();
//        if (isPrintable(text)) {
//            return text;
//        }
//        // 不可打印的显示为十六进制
//        return bytesToHex(termBytes);
//    }
//
//    /**
//     * 判断字符串是否为可打印字符（排除控制字符）
//     */
//    private static boolean isPrintable(String text) {
//        for (int i = 0; i < text.length(); i++) {
//            char c = text.charAt(i);
//            if (c < 0x20 && c != '\t' && c != '\n' && c != '\r') {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    /**
//     * 将 BytesRef 转换为十六进制字符串表示
//     */
//    private static String bytesToHex(BytesRef bytes) {
//        StringBuilder sb = new StringBuilder("0x[");
//        for (int i = bytes.offset; i < bytes.offset + bytes.length; i++) {
//            if (i > bytes.offset) sb.append(" ");
//            sb.append(String.format("%02X", bytes.bytes[i] & 0xFF));
//        }
//        sb.append("]");
//        return sb.toString();
//    }
//}