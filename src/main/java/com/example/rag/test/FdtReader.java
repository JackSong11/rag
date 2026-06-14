package com.example.rag.test;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.index.IndexableField;
import org.elasticsearch.index.mapper.Uid;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 解析 Lucene 索引中 _9.fdt 文件的内容（即 stored fields 数据）
 * 通过 DirectoryReader 打开索引目录，遍历 segment _9 的所有文档，
 * 将每个文档的所有 stored field 内容输出到文件中。
 */
public class FdtReader {

    private static final String INDEX_PATH = "/Users/songzhiquan1/IdeaProjects/rag/data/index9";
    private static final String OUTPUT_PATH = "/Users/songzhiquan1/IdeaProjects/rag/data/output/_9_fdt_content.txt";

    public static void main(String[] args) {
        try {
            // 确保输出目录存在
            Path outputDir = Paths.get(OUTPUT_PATH).getParent();
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            try (FSDirectory dir = FSDirectory.open(Paths.get(INDEX_PATH));
                 IndexReader reader = DirectoryReader.open(dir);
                 BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_PATH))) {

                writer.write("==========================================================");
                writer.newLine();
                writer.write(" 解析索引目录: " + INDEX_PATH);
                writer.newLine();
                writer.write(" 目标: _9.fdt (Stored Fields Data)");
                writer.newLine();
                writer.write("==========================================================");
                writer.newLine();
                writer.newLine();

                int totalDocs = 0;

                // 遍历所有段 (segments)
                for (LeafReaderContext leafContext : reader.leaves()) {
                    var leafReader = leafContext.reader();
                    String segmentName = leafReader.toString();

                    writer.write("[Segment: " + segmentName + "]");
                    writer.newLine();
                    writer.write("  段内最大文档数: " + leafReader.maxDoc());
                    writer.newLine();
                    writer.write("  段内有效文档数: " + leafReader.numDocs());
                    writer.newLine();
                    writer.newLine();

                    Bits liveDocs = leafReader.getLiveDocs();
                    StoredFields storedFields = leafReader.storedFields();

                    int maxDoc = leafReader.maxDoc();
                    for (int docId = 0; docId < maxDoc; docId++) {
                        // 跳过已删除的文档
                        if (liveDocs != null && !liveDocs.get(docId)) {
                            continue;
                        }

                        Document doc = storedFields.document(docId);
                        int globalDocId = leafContext.docBase + docId;

                        writer.write("----------------------------------------------------------");
                        writer.newLine();
                        writer.write("Document ID (global): " + globalDocId + " | (segment-local): " + docId);
                        writer.newLine();
                        writer.write("----------------------------------------------------------");
                        writer.newLine();

                        // 遍历文档的所有字段
                        for (IndexableField field : doc.getFields()) {
                            String fieldName = field.name();
                            String stringValue = field.stringValue();
                            BytesRef bytesValue = field.binaryValue();
                            Number numValue = field.numericValue();

                            writer.write("  [Field] " + fieldName + ":");
                            writer.newLine();

                            if (stringValue != null) {
                                // 对于长文本，截取前2000字符显示
                                if (stringValue.length() > 2000) {
                                    writer.write("    (String, length=" + stringValue.length() + ") ");
                                    writer.write(stringValue.substring(0, 2000));
                                    writer.write("... [TRUNCATED]");
                                } else {
                                    writer.write("    (String) " + stringValue);
                                }
                                writer.newLine();
                            } else if (bytesValue != null) {
                                if ("_id".equals(fieldName)) {
                                    // _id 字段使用 ES 的 Uid.decodeId 解码
                                    String id = Uid.decodeId(bytesValue.bytes, bytesValue.offset, bytesValue.length);
                                    writer.write("    " + id);
                                } else {
                                    // 其他二进制字段直接转 UTF-8
                                    String decoded = new String(bytesValue.bytes, bytesValue.offset, bytesValue.length, java.nio.charset.StandardCharsets.UTF_8);
                                    writer.write("    " + decoded);
                                }
                                writer.newLine();
                            } else if (numValue != null) {
                                writer.write("    (Numeric) " + numValue);
                                writer.newLine();
                            } else {
                                writer.write("    (null/empty)");
                                writer.newLine();
                            }
                        }

                        writer.newLine();
                        totalDocs++;
                    }
                }

                writer.newLine();
                writer.write("==========================================================");
                writer.newLine();
                writer.write(" 解析完成! 共输出 " + totalDocs + " 个有效文档的 stored fields 内容");
                writer.newLine();
                writer.write("==========================================================");
                writer.newLine();

                System.out.println("解析完成! 共输出 " + totalDocs + " 个文档");
                System.out.println("输出文件: " + OUTPUT_PATH);
            }

        } catch (IOException e) {
            System.err.println("解析失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}