package com.chatbot.handoff.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticCacheService {

    private static final double SIMILARITY_THRESHOLD = 0.85;

    private final EmbeddingModel embeddingModel;
    private final Map<float[], String> cache = new ConcurrentHashMap<>();

    public CacheResult lookup(String query) {
        float[] queryEmbedding = embeddingModel.embed(query);

        for (Map.Entry<float[], String> entry : cache.entrySet()) {
            double similarity = cosineSimilarity(queryEmbedding, entry.getKey());
            if (similarity >= SIMILARITY_THRESHOLD) {
                log.info("시맨틱 캐시 히트 (유사도: {})", String.format("%.4f", similarity));
                return new CacheResult(entry.getValue(), null);
            }
        }
        return new CacheResult(null, queryEmbedding);
    }

    public void cacheAnswer(float[] queryEmbedding, String answer) {
        cache.put(queryEmbedding, answer);
        log.info("캐시 저장 완료 (현재 캐시 크기: {})", cache.size());
    }

    public record CacheResult(String answer, float[] embedding) {
        public boolean isHit() {
            return answer != null;
        }
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
