package com.pdfanalyzer.core.service;

import com.pdfanalyzer.core.integration.llm.LLMAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class AnalysisCacheService {

    // LRU 캐시 구현 (최대 100개 항목)
    private static final int MAX_CACHE_SIZE = 100;

    private final Map<String, CachedAnalysis> cache = new LinkedHashMap<String, CachedAnalysis>(MAX_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedAnalysis> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    /**
     * 문서 내용으로부터 고유 해시 생성
     */
    public String generateDocumentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 알고리즘을 찾을 수 없습니다", e);
            return null;
        }
    }

    /**
     * 캐시에서 분석 결과 조회
     */
    public CachedAnalysis get(String documentHash) {
        CachedAnalysis cached = cache.get(documentHash);
        if (cached != null) {
            log.info("캐시 히트: {}", documentHash.substring(0, 8) + "...");
            cached.incrementHits();
        }
        return cached;
    }

    /**
     * 캐시에 분석 결과 저장
     */
    public void put(String documentHash, String summary, java.util.List<String> keywords,
                    Map<String, java.util.List<LLMAdapter.KeywordLocation>> keywordLocations) {
        CachedAnalysis analysis = new CachedAnalysis(summary, keywords, keywordLocations);
        cache.put(documentHash, analysis);
        log.info("캐시 저장: {} (현재 캐시 크기: {})", documentHash.substring(0, 8) + "...", cache.size());
    }

    /**
     * 캐시 통계 조회
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", cache.size());
        stats.put("maxCacheSize", MAX_CACHE_SIZE);

        int totalHits = cache.values().stream()
            .mapToInt(CachedAnalysis::getHits)
            .sum();
        stats.put("totalCacheHits", totalHits);

        return stats;
    }

    /**
     * 캐시 초기화
     */
    public void clear() {
        cache.clear();
        log.info("캐시가 초기화되었습니다");
    }

    /**
     * 캐시된 분석 결과
     */
    public static class CachedAnalysis {
        private final String summary;
        private final java.util.List<String> keywords;
        private final Map<String, java.util.List<LLMAdapter.KeywordLocation>> keywordLocations;
        private final long timestamp;
        private int hits;

        public CachedAnalysis(String summary, java.util.List<String> keywords,
                            Map<String, java.util.List<LLMAdapter.KeywordLocation>> keywordLocations) {
            this.summary = summary;
            this.keywords = keywords;
            this.keywordLocations = keywordLocations;
            this.timestamp = System.currentTimeMillis();
            this.hits = 0;
        }

        public String getSummary() {
            return summary;
        }

        public java.util.List<String> getKeywords() {
            return keywords;
        }

        public Map<String, java.util.List<LLMAdapter.KeywordLocation>> getKeywordLocations() {
            return keywordLocations;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public int getHits() {
            return hits;
        }

        public void incrementHits() {
            this.hits++;
        }
    }
}
