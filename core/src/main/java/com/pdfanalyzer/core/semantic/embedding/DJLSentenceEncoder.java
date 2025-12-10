package com.pdfanalyzer.core.semantic.embedding;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;

/**
 * DJL 기반 Sentence-BERT 임베딩 인코더
 * 실제 의미론적 유사성을 반영하는 임베딩 벡터 생성
 */
@Slf4j
@Component
public class DJLSentenceEncoder {

    @Value("${djl.embedding.enabled:true}")
    private boolean enabled;

    @Value("${djl.embedding.model:sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2}")
    private String modelName;

    @Value("${djl.embedding.dimension:384}")
    private int embeddingDimension;

    private ZooModel<String, float[]> model;
    private Predictor<String, float[]> predictor;

    @PostConstruct
    public void initialize() {
        if (!enabled) {
            log.info("DJL 임베딩이 비활성화되어 있습니다. 폴백 모드로 동작합니다.");
            return;
        }

        try {
            log.info("DJL Sentence-BERT 모델 로딩 시작: {}", modelName);
            long startTime = System.currentTimeMillis();

            // HuggingFace에서 Sentence-BERT 모델 로드
            Criteria<String, float[]> criteria = Criteria.builder()
                    .setTypes(String.class, float[].class)
                    .optModelUrls("djl://ai.djl.huggingface.pytorch/" + modelName)
                    .optEngine("PyTorch")
                    .optProgress(new ProgressBar())
                    .build();

            model = criteria.loadModel();
            predictor = model.newPredictor();

            long loadTime = System.currentTimeMillis() - startTime;
            log.info("✅ DJL 모델 로딩 완료: {}ms, 임베딩 차원: {}", loadTime, embeddingDimension);

            // 워밍업 (첫 예측은 느리므로)
            warmup();

        } catch (ModelNotFoundException | MalformedModelException | IOException e) {
            log.error("❌ DJL 모델 로딩 실패. 폴백 모드로 전환합니다.", e);
            enabled = false;
        }
    }

    /**
     * 텍스트를 의미론적 임베딩 벡터로 변환
     */
    public double[] encode(String text) {
        if (!enabled || predictor == null) {
            log.debug("DJL 비활성화 상태, null 반환");
            return null;
        }

        try {
            // 텍스트 정제
            String cleanedText = text.trim();
            if (cleanedText.isEmpty()) {
                return getZeroEmbedding();
            }

            // 너무 긴 텍스트는 잘라냄 (BERT 최대 토큰 512)
            if (cleanedText.length() > 2000) {
                cleanedText = cleanedText.substring(0, 2000);
            }

            // DJL 예측
            float[] embedding = predictor.predict(cleanedText);

            // float[] -> double[] 변환
            double[] result = new double[embedding.length];
            for (int i = 0; i < embedding.length; i++) {
                result[i] = embedding[i];
            }

            log.debug("임베딩 생성 완료: {} chars -> {} dim", text.length(), result.length);
            return result;

        } catch (TranslateException e) {
            log.error("임베딩 생성 중 오류: {}", text.substring(0, Math.min(50, text.length())), e);
            return null;
        }
    }

    /**
     * 여러 문장을 한 번에 임베딩 (배치 처리)
     */
    public double[][] encodeBatch(String[] texts) {
        if (!enabled || predictor == null) {
            return null;
        }

        double[][] results = new double[texts.length][];
        for (int i = 0; i < texts.length; i++) {
            results[i] = encode(texts[i]);
        }
        return results;
    }

    /**
     * 모델 워밍업 (첫 예측 속도 개선)
     */
    private void warmup() {
        try {
            log.info("모델 워밍업 중...");
            encode("This is a warmup sentence.");
            encode("이것은 워밍업 문장입니다.");
            log.info("워밍업 완료");
        } catch (Exception e) {
            log.warn("워밍업 실패", e);
        }
    }

    /**
     * 0 벡터 반환 (폴백)
     */
    private double[] getZeroEmbedding() {
        double[] zeros = new double[embeddingDimension];
        return zeros;
    }

    /**
     * 모델이 사용 가능한지 확인
     */
    public boolean isAvailable() {
        return enabled && predictor != null;
    }

    /**
     * 임베딩 차원 반환
     */
    public int getEmbeddingDimension() {
        return embeddingDimension;
    }

    @PreDestroy
    public void cleanup() {
        if (predictor != null) {
            predictor.close();
            log.info("Predictor 종료");
        }
        if (model != null) {
            model.close();
            log.info("DJL 모델 종료");
        }
    }
}
