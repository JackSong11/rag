package com.example.rag.test;

import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Paths;

/**
 * 解析 /Users/songzhiquan1/IdeaProjects/rag/data/index9/_9.fdx 文件内容
 * 并将解析结果保存到 data/output/_9_fdx_content.txt
 *
 * .fdx 文件是 Lucene 存储字段索引文件 (Stored Fields Index)，
 * 用于记录每个 chunk 在 .fdt 文件中的偏移量，以便快速定位文档的存储字段数据。
 */
public class Fdx9Reader {

    private static final String INDEX_PATH = "/Users/songzhiquan1/IdeaProjects/rag/data/index9/";
    private static final String FILE_NAME = "_9.fdx";
    private static final String OUTPUT_PATH = "/Users/songzhiquan1/IdeaProjects/rag/data/output/_9_fdx_content.txt";

    public static void main(String[] args) {
        try (Directory directory = FSDirectory.open(Paths.get(INDEX_PATH));
             IndexInput input = directory.openInput(FILE_NAME, IOContext.READONCE);
             PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_PATH))) {

            long fileLength = input.length();
            writer.println("=== Lucene _9.fdx 文件解析 (Stored Fields Index) ===");
            writer.println("文件路径: " + INDEX_PATH + FILE_NAME);
            writer.println("文件长度: " + fileLength + " bytes");
            writer.println();

            // 1. 解析 Codec Header
            writer.println("--- Codec Header ---");
            int magic = input.readInt();
            writer.printf("Magic Number: 0x%08X (期望: 0x3FD76C17 即 CodecUtil.CODEC_MAGIC)%n", magic);

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

            long headerEndPos = input.getFilePointer();
            writer.println("Header 结束位置 (offset): " + headerEndPos);
            writer.println();

            // 2. 解析 FDX 数据区域
            writer.println("--- FDX 数据区域 (Chunk 到 .fdt 的偏移指针) ---");

            // Footer 占 16 bytes (magic 4 + algorithmID 4 + checksum 8)
            long footerStart = fileLength - 16;
            writer.println("数据区域: offset " + headerEndPos + " ~ " + (footerStart - 1));
            writer.println("数据区域长度: " + (footerStart - headerEndPos) + " bytes");
            writer.println();

            // 以 VInt/VLong 方式尝试读取数据
            writer.println("--- 以 VInt/VLong 方式解析数据区域 ---");
            long dataStart = input.getFilePointer();
            int index = 0;

            // 首先尝试读取 numChunks
            if (input.getFilePointer() < footerStart) {
                long offset = input.getFilePointer();
                int numChunks = input.readVInt();
                writer.printf("[%3d] Offset: %4d | numChunks (数据块总数): %d%n", index, offset, numChunks);
                index++;

                // 读取后续的指针数据
                int chunkIdx = 0;
                while (input.getFilePointer() < footerStart && chunkIdx < numChunks) {
                    offset = input.getFilePointer();
                    try {
                        long fdtOffset = input.readVLong();
                        writer.printf("[%3d] Offset: %4d | Chunk #%d -> .fdt 偏移量: %d%n",
                                index, offset, chunkIdx, fdtOffset);
                        index++;
                        chunkIdx++;
                    } catch (Exception e) {
                        writer.printf("[%3d] Offset: %4d | 读取VLong失败: %s%n", index, offset, e.getMessage());
                        input.seek(offset + 1);
                        index++;
                        chunkIdx++;
                    }
                }

                // 如果还有剩余数据，继续读取
                while (input.getFilePointer() < footerStart) {
                    offset = input.getFilePointer();
                    try {
                        long val = input.readVLong();
                        writer.printf("[%3d] Offset: %4d | 额外数据 VLong: %d%n", index, offset, val);
                        index++;
                    } catch (Exception e) {
                        writer.printf("[%3d] Offset: %4d | 读取失败: %s%n", index, offset, e.getMessage());
                        input.seek(offset + 1);
                        index++;
                    }
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