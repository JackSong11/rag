package com.example.rag.test;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class TaskFingerprintManager {

    /**
     * 计算任务指纹 (task_digest)
     * * @param chunkingConfig 切片配置 Map
     * @param field          当前处理的字段
     * @param docId          文档 ID
     * @param pageRange      页码范围（例如 "1-10"）
     * @return 64位十六进制哈希字符串
     */
    public static String calculateTaskDigest(Map<String, Object> chunkingConfig, String field, String docId, String pageRange) {
        // 1. 获取一个 xxhash64 算法实例 (Guava 提供了优质的 xxhash64 实现)
        HashFunction xxhash64 = Hashing.fingerprint2011(); 
        // 注：如果追求绝对的 xxhash 性能，Guava 中通常使用 Hashing.farmHash64() 或用专门的 xxhash-bn接口。
        // 这里以 Guava 的 64位高效哈希为例：
        
        // 2. 创建一个 Hasher（相当于 Python 的 hasher = xxhash.xxh64()）
        // 在 Java 中，Hasher 是一个流式构建器（Builder 模式）
        Hasher hasher = xxhash64.newHasher();

        // 3. 混合切片配置 + doc_id + 页码范围 (流式追加数据)
        Object configValue = chunkingConfig.get(field);
        if (configValue != null) {
            hasher.putString(configValue.toString(), StandardCharsets.UTF_8);
        }
        hasher.putString(docId, StandardCharsets.UTF_8);
        hasher.putString(pageRange, StandardCharsets.UTF_8);

        // 4. 计算并返回十六进制字符串（相当于 Python 的 hasher.hexdigest()）
        return hasher.hash().toString();
    }

    public static void main(String[] args) {
        // 模拟测试数据
        Map<String, Object> mockConfig = Map.of("content", "size=500;overlap=50");
        String field = "content";
        String docId = "doc_abc123";
        String pageRange = "1-5";

        // 计算指纹
        String taskDigest = calculateTaskDigest(mockConfig, field, docId, pageRange);
        
        System.out.println("生成的任务指纹 (Task Digest): " + taskDigest);
    }
}