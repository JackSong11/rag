package com.example.rag.test;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Paths;

/**
 * 解析 /Users/songzhiquan1/IdeaProjects/rag/data/index9/_9.fdm 文件内容
 * 并将解析结果保存到 data/output/_9_fdm_content.txt
 */
public class Fdm9Reader {

    private static final String INDEX_PATH = "/Users/songzhiquan1/IdeaProjects/rag/data/index9/";
    private static final String FILE_NAME = "_9.fdm";
    private static final String OUTPUT_PATH = "/Users/songzhiquan1/IdeaProjects/rag/data/output/_9_fdm_content.txt";

    public static void main(String[] args) {
        try (Directory directory = FSDirectory.open(Paths.get(INDEX_PATH));
             IndexInput input = directory.openInput(FILE_NAME, IOContext.READONCE);
             PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_PATH))) {

            long fileLength = input.length();
            writer.println("=== Lucene _9.fdm 文件解析 ===");
            writer.println("文件路径: " + INDEX_PATH + FILE_NAME);
            writer.println("文件长度: " + fileLength + " bytes");
            writer.println();

            // 1. 解析 Codec Header
            writer.println("--- Codec Header ---");
            int magic = input.readInt();
            writer.printf("Magic Number: 0x%08X (期望: 0x3FD76C17 即 CodecUtil.CODEC_MAGIC)%n", magic);

            // 读取 codec name (UTF-8 string, 前缀为长度)
            String codecName = input.readString();
            writer.println("Codec Name: " + codecName);

            int version = input.readInt();
            writer.println("Version: " + version);

            // 读取 Segment ID (16 bytes)
            byte[] segmentId = new byte[16];
            input.readBytes(segmentId, 0, 16);
            writer.print("Segment ID: ");
            StringBuilder sb = new StringBuilder();
            for (byte b : segmentId) {
                sb.append(String.format("%02X", b));
            }
            writer.println(sb.toString());

            // 读取 Suffix
            String suffix = input.readString();
            writer.println("Suffix: \"" + suffix + "\"");
            writer.println();

            // 2. 解析 FDM 元数据内容
            writer.println("--- FDM 元数据内容 ---");
            writer.println("Header 结束位置 (offset): " + input.getFilePointer());
            writer.println();

            // Footer 占 16 bytes (magic 4 + algorithmID 4 + checksum 8)
            long footerStart = fileLength - 16;

            writer.println("数据区域: offset " + input.getFilePointer() + " ~ " + (footerStart - 1));
            writer.println();

            // 逐字节和逐VInt方式解析数据区域
            // 先用 VInt 方式读取
            writer.println("--- 以 VInt 方式读取数据区域---");
            long dataStart = input.getFilePointer();

            int index = 0;
            while (input.getFilePointer() < footerStart) {
                long offset = input.getFilePointer();
                try {
                    int val = input.readVInt();
                    writer.printf("[%3d] Offset: %4d | VInt值: %d%n", index, offset, val);
                    index++;
                } catch (Exception e) {
                    writer.printf("[%3d] Offset: %4d | 读取VInt失败: %s%n", index, offset, e.getMessage());
                    // 跳过一个字节继续
                    input.seek(offset + 1);
                    index++;
                }
            }
            writer.println();

            // 3. 再以原始字节方式输出
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

            // 4. 解析 Footer
            writer.println("--- Codec Footer ---");
            input.seek(footerStart);
            int footerMagic = input.readInt();
            writer.printf("Footer Magic: 0x%08X (期望: 0xFFFFFFFE 即 CodecUtil.FOOTER_MAGIC)%n", footerMagic);

            int algorithmID = input.readInt();
            writer.println("Algorithm ID: " + algorithmID);

            long checksum = input.readLong();
            writer.printf("Checksum: %d (0x%016X)%n", checksum, checksum);

            writer.println();
            writer.println("=== 解析完成 ===");

            System.out.println("解析完成！结果已保存到: " + OUTPUT_PATH);

        } catch (Exception e) {
            System.err.println("解析失败:");
            e.printStackTrace();
        }
    }
}