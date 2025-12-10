package com.pdfanalyzer.core.ml.scoring;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.Batchifier;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import com.pdfanalyzer.core.semantic.model.SemanticCell;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * DJL 기반 구조적 점수 예측 모델
 * 고정 가중치를 ML 기반 예측으로 대체
 */
@Slf4j
@Component
public class StructuralScorePredictor {

    @Value("${djl.score.enabled:false}")
    private boolean enabled;

    @Value("${djl.score.model.path:models/score_predictor.pt}")
    private String modelPath;

    private ZooModel<float[], Float> model;
    private Predictor<float[], Float> predictor;

    @PostConstruct
    public void initialize() {
        if (!enabled) {
            log.info("DJL 점수 예측 모델이 비활성화되어 있습니다. 규칙 기반 점수 사용.");
            return;
        }

        Path modelFile = Paths.get(modelPath);
        if (!Files.exists(modelFile)) {
            log.warn("점수 예측 모델 파일이 없습니다: {}. 규칙 기반으로 폴백합니다.", modelPath);
            enabled = false;
            return;
        }

        try {
            log.info("DJL 점수 예측 모델 로딩 시작: {}", modelPath);
            long startTime = System.currentTimeMillis();

            Criteria<float[], Float> criteria = Criteria.builder()
                    .setTypes(float[].class, Float.class)
                    .optModelPath(modelFile.getParent())
                    .optModelName(modelFile.getFileName().toString().replace(".pt", ""))
                    .optTranslator(new ScorePredictorTranslator())
                    .optProgress(new ProgressBar())
                    .optEngine("PyTorch")
                    .build();

            model = criteria.loadModel();
            predictor = model.newPredictor();

            long loadTime = System.currentTimeMillis() - startTime;
            log.info("✅ DJL 점수 예측 모델 로딩 완료: {}ms", loadTime);

        } catch (ModelNotFoundException | MalformedModelException | IOException e) {
            log.error("❌ DJL 점수 예측 모델 로딩 실패. 규칙 기반으로 폴백합니다.", e);
            enabled = false;
        }
    }

    /**
     * 구조적 점수 예측
     */
    public Double predictScore(SemanticCell cell, double resonance) {
        if (!enabled || predictor == null) {
            return null; // 폴백으로 규칙 기반 사용
        }

        try {
            // 특징 추출 (7개 특징)
            float[] features = extractFeatures(cell, resonance);

            // DJL 예측
            Float score = predictor.predict(features);

            // 0~1 범위로 클리핑
            double clippedScore = Math.max(0.0, Math.min(1.0, score));

            log.debug("DJL 점수 예측: {:.3f} (원본: {:.3f})", clippedScore, score);

            return clippedScore;

        } catch (TranslateException e) {
            log.error("DJL 점수 예측 중 오류 발생", e);
            return null; // 폴백
        }
    }

    /**
     * 배치 예측 (여러 셀을 한 번에)
     */
    public double[] predictScoreBatch(SemanticCell[] cells, double[] resonances) {
        if (!enabled || predictor == null || cells.length == 0) {
            return null;
        }

        double[] scores = new double[cells.length];
        for (int i = 0; i < cells.length; i++) {
            Double score = predictScore(cells[i], resonances[i]);
            scores[i] = score != null ? score : 0.0;
        }

        return scores;
    }

    /**
     * 셀 특징 추출
     */
    private float[] extractFeatures(SemanticCell cell, double resonance) {
        SemanticCell.CellLayoutInfo layoutInfo = cell.getLayoutInfo();

        return new float[]{
                (float) cell.getImportance(),                    // 특징 1: 기본 중요도
                (float) resonance,                               // 특징 2: 공명 강도
                cell.isHeader() ? 1.0f : 0.0f,                  // 특징 3: 헤더 여부
                (float) cell.getLength() / 1000.0f,             // 특징 4: 셀 길이 (정규화)
                (float) layoutInfo.getRelativeFontSize(),       // 특징 5: 상대 폰트 크기
                (float) layoutInfo.getIndentLevel() / 10.0f,    // 특징 6: 들여쓰기 레벨
                (float) layoutInfo.getWordCount() / 100.0f      // 특징 7: 단어 수 (정규화)
        };
    }

    /**
     * 규칙 기반 점수 계산 (폴백용)
     */
    public double calculateRuleBasedScore(SemanticCell cell, double resonance) {
        double base = cell.getImportance();
        double normalized = Math.min(1.0, resonance / 10.0);
        double score = (base * 0.4) + (normalized * 0.6);
        return cell.isHeader() ? Math.min(1.0, score * 1.2) : score;
    }

    /**
     * 하이브리드 점수 계산 (ML + 규칙 앙상블)
     */
    public double calculateHybridScore(SemanticCell cell, double resonance, double weight) {
        Double mlScore = predictScore(cell, resonance);
        double ruleScore = calculateRuleBasedScore(cell, resonance);

        if (mlScore != null) {
            // 가중 평균
            return mlScore * weight + ruleScore * (1 - weight);
        }

        return ruleScore;
    }

    /**
     * 모델 사용 가능 여부
     */
    public boolean isAvailable() {
        return enabled && predictor != null;
    }

    @PreDestroy
    public void cleanup() {
        if (predictor != null) {
            predictor.close();
            log.info("Score Predictor 종료");
        }
        if (model != null) {
            model.close();
            log.info("DJL 점수 예측 모델 종료");
        }
    }

    /**
     * DJL Translator
     */
    private static class ScorePredictorTranslator implements Translator<float[], Float> {

        @Override
        public NDList processInput(TranslatorContext ctx, float[] input) {
            NDManager manager = ctx.getNDManager();
            NDArray array = manager.create(input);
            return new NDList(array);
        }

        @Override
        public Float processOutput(TranslatorContext ctx, NDList list) {
            NDArray output = list.singletonOrThrow();
            return output.getFloat();
        }

        @Override
        public Batchifier getBatchifier() {
            return Batchifier.STACK;
        }
    }
}
