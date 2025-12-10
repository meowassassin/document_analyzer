package com.pdfanalyzer.core.semantic.embedding;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DJL Sentence Encoder 테스트
 */
@SpringBootTest
@TestPropertySource(properties = {
    "djl.embedding.enabled=true"
})
class DJLSentenceEncoderTest {

    @Autowired(required = false)
    private DJLSentenceEncoder encoder;

    @Autowired
    private TokenMixer tokenMixer;

    @Test
    void testEncoderAvailability() {
        if (encoder != null && encoder.isAvailable()) {
            System.out.println("✅ DJL Encoder 사용 가능");
            assertTrue(encoder.isAvailable());
        } else {
            System.out.println("⚠️ DJL Encoder 사용 불가 (폴백 모드)");
        }
    }

    @Test
    void testSentenceEmbedding() {
        String text = "This is a test sentence for embedding.";
        double[] embedding = tokenMixer.createSentenceEmbedding(text);

        assertNotNull(embedding);
        assertTrue(embedding.length > 0);
        System.out.printf("임베딩 차원: %d%n", embedding.length);

        if (tokenMixer.isDJLEnabled()) {
            System.out.println("✅ DJL Sentence-BERT 임베딩 사용 중");
            assertEquals(384, embedding.length); // Sentence-BERT 차원
        } else {
            System.out.println("⚠️ 폴백 해시 임베딩 사용 중");
            assertEquals(128, embedding.length); // 폴백 임베딩 차원
        }
    }

    @Test
    void testSemanticSimilarity() {
        if (!tokenMixer.isDJLEnabled()) {
            System.out.println("⏭️ DJL 비활성화, 테스트 스킵");
            return;
        }

        double[] embedding1 = tokenMixer.createSentenceEmbedding("I love programming");
        double[] embedding2 = tokenMixer.createSentenceEmbedding("I enjoy coding");
        double[] embedding3 = tokenMixer.createSentenceEmbedding("The weather is nice");

        // 코사인 유사도 계산
        double similarity12 = cosineSimilarity(embedding1, embedding2);
        double similarity13 = cosineSimilarity(embedding1, embedding3);

        System.out.printf("유사도 (programming vs coding): %.3f%n", similarity12);
        System.out.printf("유사도 (programming vs weather): %.3f%n", similarity13);

        // 의미가 비슷한 문장이 더 높은 유사도를 가져야 함
        assertTrue(similarity12 > similarity13,
            "의미가 유사한 문장의 유사도가 더 높아야 합니다");
    }

    @Test
    void testKoreanEmbedding() {
        if (!tokenMixer.isDJLEnabled()) {
            System.out.println("⏭️ DJL 비활성화, 테스트 스킵");
            return;
        }

        double[] embedding1 = tokenMixer.createSentenceEmbedding("안녕하세요");
        double[] embedding2 = tokenMixer.createSentenceEmbedding("반갑습니다");
        double[] embedding3 = tokenMixer.createSentenceEmbedding("사과는 맛있다");

        assertNotNull(embedding1);
        assertNotNull(embedding2);
        assertNotNull(embedding3);

        double similarity12 = cosineSimilarity(embedding1, embedding2);
        double similarity13 = cosineSimilarity(embedding1, embedding3);

        System.out.printf("유사도 (안녕 vs 반갑): %.3f%n", similarity12);
        System.out.printf("유사도 (안녕 vs 사과): %.3f%n", similarity13);

        assertTrue(similarity12 > similarity13,
            "인사말끼리의 유사도가 더 높아야 합니다");
    }

    private double cosineSimilarity(double[] vec1, double[] vec2) {
        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException("벡터 차원이 일치하지 않습니다");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
