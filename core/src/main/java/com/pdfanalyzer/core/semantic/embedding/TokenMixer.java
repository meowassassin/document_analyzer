package com.pdfanalyzer.core.semantic.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Token Mixing 알고리즘
 * E'ᵢ = (Eᵢ₋₁ + Eᵢ + Eᵢ₊₁) / 3
 * 인접 토큰의 임베딩을 평균화하여 의미 손실을 최소화
 *
 * DJL 통합: 실제 Sentence-BERT 임베딩 또는 폴백 해시 기반 임베딩 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenMixer {

    private final DJLSentenceEncoder djlEncoder;

    @Value("${djl.embedding.enabled:true}")
    private boolean djlEnabled;

    public double[][] tokenize(String text) {
        String[] words = text.trim().split("\\s+");
        double[][] embeddings = new double[words.length][];

        for (int i = 0; i < words.length; i++) {
            embeddings[i] = createSimpleEmbedding(words[i]);
        }

        log.debug("토큰화 완료: {} 단어", words.length);
        return embeddings;
    }

    public double[][] mixTokens(double[][] embeddings) {
        if (embeddings == null || embeddings.length == 0) {
            return new double[0][];
        }

        int n = embeddings.length;
        int dim = embeddings[0].length;
        double[][] mixed = new double[n][dim];

        for (int i = 0; i < n; i++) {
            for (int d = 0; d < dim; d++) {
                double sum = embeddings[i][d];
                int count = 1;

                if (i > 0) {
                    sum += embeddings[i - 1][d];
                    count++;
                }

                if (i < n - 1) {
                    sum += embeddings[i + 1][d];
                    count++;
                }

                mixed[i][d] = sum / count;
            }
        }

        log.debug("Token Mixing 완료: {} 토큰", n);
        return mixed;
    }

    public double[] averageEmbeddings(double[][] embeddings) {
        if (embeddings == null || embeddings.length == 0) {
            return new double[0];
        }

        int dim = embeddings[0].length;
        double[] averaged = new double[dim];

        for (double[] embedding : embeddings) {
            for (int d = 0; d < dim; d++) {
                averaged[d] += embedding[d];
            }
        }

        for (int d = 0; d < dim; d++) {
            averaged[d] /= embeddings.length;
        }

        return averaged;
    }

    public double[] createSentenceEmbedding(String text) {
        // DJL 임베딩 사용 (우선순위)
        if (djlEnabled && djlEncoder.isAvailable()) {
            double[] djlEmbedding = djlEncoder.encode(text);
            if (djlEmbedding != null) {
                log.debug("✅ DJL Sentence-BERT 임베딩 사용: {} chars -> {} dim",
                         text.length(), djlEmbedding.length);
                return djlEmbedding;
            }
        }

        // 폴백: 기존 해시 기반 임베딩
        log.debug("⚠️ 폴백 해시 기반 임베딩 사용");
        double[][] tokenEmbeddings = tokenize(text);
        double[][] mixedEmbeddings = mixTokens(tokenEmbeddings);
        return averageEmbeddings(mixedEmbeddings);
    }

    /**
     * 폴백용 해시 기반 임베딩 (DJL 사용 불가 시)
     * ⚠️ 주의: 실제 의미를 반영하지 않는 가짜 임베딩
     */
    private double[] createSimpleEmbedding(String word) {
        int embeddingDim = 128;
        double[] embedding = new double[embeddingDim];

        int hash = word.hashCode();
        for (int i = 0; i < embeddingDim; i++) {
            embedding[i] = Math.sin(hash * (i + 1)) * Math.cos(hash / (i + 1.0));
        }

        // L2 정규화
        double norm = 0;
        for (double v : embedding) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        for (int i = 0; i < embeddingDim; i++) {
            embedding[i] /= norm;
        }

        return embedding;
    }

    /**
     * DJL 임베딩이 활성화되어 있는지 확인
     */
    public boolean isDJLEnabled() {
        return djlEnabled && djlEncoder.isAvailable();
    }

    /**
     * 현재 임베딩 차원 반환
     */
    public int getEmbeddingDimension() {
        if (isDJLEnabled()) {
            return djlEncoder.getEmbeddingDimension();
        }
        return 128; // 폴백 임베딩 차원
    }
}
