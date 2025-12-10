package com.pdfanalyzer.core.ml.classification;

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
import com.pdfanalyzer.core.frequency.filter.DocumentType;
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
import java.util.List;

/**
 * DJL 기반 문서 타입 분류기
 * 규칙 기반 분류를 ML 기반으로 대체
 */
@Slf4j
@Component
public class DocumentClassifier {

    @Value("${djl.classifier.enabled:false}")
    private boolean enabled;

    @Value("${djl.classifier.model.path:models/document_classifier.pt}")
    private String modelPath;

    @Value("${djl.classifier.confidence.threshold:0.7}")
    private double confidenceThreshold;

    private ZooModel<float[], Classifications> model;
    private Predictor<float[], Classifications> predictor;

    @PostConstruct
    public void initialize() {
        if (!enabled) {
            log.info("DJL 문서 분류기가 비활성화되어 있습니다. 규칙 기반 분류 사용.");
            return;
        }

        Path modelFile = Paths.get(modelPath);
        if (!Files.exists(modelFile)) {
            log.warn("모델 파일이 없습니다: {}. 규칙 기반 분류로 폴백합니다.", modelPath);
            enabled = false;
            return;
        }

        try {
            log.info("DJL 문서 분류 모델 로딩 시작: {}", modelPath);
            long startTime = System.currentTimeMillis();

            Criteria<float[], Classifications> criteria = Criteria.builder()
                    .setTypes(float[].class, Classifications.class)
                    .optModelPath(modelFile.getParent())
                    .optModelName(modelFile.getFileName().toString().replace(".pt", ""))
                    .optTranslator(new DocumentClassifierTranslator())
                    .optProgress(new ProgressBar())
                    .optEngine("PyTorch")
                    .build();

            model = criteria.loadModel();
            predictor = model.newPredictor();

            long loadTime = System.currentTimeMillis() - startTime;
            log.info("✅ DJL 문서 분류 모델 로딩 완료: {}ms", loadTime);

        } catch (ModelNotFoundException | MalformedModelException | IOException e) {
            log.error("❌ DJL 문서 분류 모델 로딩 실패. 규칙 기반으로 폴백합니다.", e);
            enabled = false;
        }
    }

    /**
     * 문서 타입 예측
     */
    public ClassificationResult predict(List<SemanticCell> cells) {
        if (!enabled || predictor == null) {
            return null; // 폴백으로 규칙 기반 사용
        }

        try {
            // 특징 추출
            float[] features = extractFeatures(cells);

            // DJL 예측
            Classifications result = predictor.predict(features);

            // 가장 높은 확률의 클래스
            Classification best = result.best();

            log.debug("DJL 예측: {} (신뢰도: {:.2f})", best.className, best.probability);

            return ClassificationResult.builder()
                    .documentType(DocumentType.valueOf(best.className))
                    .confidence(best.probability)
                    .allProbabilities(result)
                    .usedML(true)
                    .build();

        } catch (TranslateException e) {
            log.error("DJL 예측 중 오류 발생", e);
            return null; // 폴백
        }
    }

    /**
     * 문서 특징 추출 (6개 특징)
     */
    private float[] extractFeatures(List<SemanticCell> cells) {
        int totalCells = cells.size();
        if (totalCells == 0) {
            return new float[6];
        }

        // 특징 1-3: BlockType 분포
        long headers = cells.stream().filter(SemanticCell::isHeader).count();
        long lists = cells.stream()
                .filter(c -> c.getType().toString().contains("LIST"))
                .count();
        long paragraphs = cells.stream()
                .filter(c -> c.getType().toString().contains("PARAGRAPH"))
                .count();

        // 특징 4: 평균 셀 길이
        double avgLength = cells.stream()
                .mapToInt(SemanticCell::getLength)
                .average()
                .orElse(0.0);

        // 특징 5: 평균 중요도
        double avgImportance = cells.stream()
                .mapToDouble(SemanticCell::getImportance)
                .average()
                .orElse(0.0);

        // 특징 6: 총 셀 개수 (로그 스케일)
        double logCellCount = Math.log(totalCells + 1);

        return new float[]{
                (float) headers / totalCells,      // 헤더 비율
                (float) lists / totalCells,        // 리스트 비율
                (float) paragraphs / totalCells,   // 문단 비율
                (float) avgLength / 1000.0f,       // 평균 길이 (정규화)
                (float) avgImportance,             // 평균 중요도
                (float) logCellCount / 10.0f       // 로그 셀 개수 (정규화)
        };
    }

    /**
     * 신뢰도가 충분한지 확인
     */
    public boolean isConfident(ClassificationResult result) {
        return result != null && result.confidence >= confidenceThreshold;
    }

    /**
     * 분류기 사용 가능 여부
     */
    public boolean isAvailable() {
        return enabled && predictor != null;
    }

    @PreDestroy
    public void cleanup() {
        if (predictor != null) {
            predictor.close();
            log.info("Predictor 종료");
        }
        if (model != null) {
            model.close();
            log.info("DJL 문서 분류 모델 종료");
        }
    }

    /**
     * 분류 결과 DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class ClassificationResult {
        private DocumentType documentType;
        private double confidence;
        private Classifications allProbabilities;
        private boolean usedML;
    }

    /**
     * 분류 결과 (다중 클래스)
     */
    public static class Classifications {
        private final Classification[] classifications;

        public Classifications(Classification[] classifications) {
            this.classifications = classifications;
        }

        public Classification best() {
            Classification best = classifications[0];
            for (Classification c : classifications) {
                if (c.probability > best.probability) {
                    best = c;
                }
            }
            return best;
        }

        public Classification[] getAll() {
            return classifications;
        }
    }

    /**
     * 단일 분류 결과
     */
    public static class Classification {
        private final String className;
        private final double probability;

        public Classification(String className, double probability) {
            this.className = className;
            this.probability = probability;
        }

        public String getClassName() {
            return className;
        }

        public double getProbability() {
            return probability;
        }
    }

    /**
     * DJL Translator
     */
    private static class DocumentClassifierTranslator implements Translator<float[], Classifications> {

        @Override
        public NDList processInput(TranslatorContext ctx, float[] input) {
            NDManager manager = ctx.getNDManager();
            NDArray array = manager.create(input);
            return new NDList(array);
        }

        @Override
        public Classifications processOutput(TranslatorContext ctx, NDList list) {
            NDArray output = list.singletonOrThrow();
            float[] probabilities = output.toFloatArray();

            // 5개 클래스
            String[] classes = {
                "RESEARCH_PAPER",
                "CONTRACT",
                "REPORT",
                "PRESENTATION",
                "GENERAL"
            };

            Classification[] results = new Classification[classes.length];
            for (int i = 0; i < classes.length; i++) {
                results[i] = new Classification(classes[i], probabilities[i]);
            }

            return new Classifications(results);
        }

        @Override
        public Batchifier getBatchifier() {
            return Batchifier.STACK;
        }
    }
}
