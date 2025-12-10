package com.pdfanalyzer.core.analysis.scoring;

import com.pdfanalyzer.core.frequency.fft.FFTEngine;
import com.pdfanalyzer.core.frequency.filter.DocumentType;
import com.pdfanalyzer.core.frequency.filter.ResonanceFilter;
import com.pdfanalyzer.core.frequency.filter.ResonanceFilterRegistry;
import com.pdfanalyzer.core.ml.classification.DocumentClassifier;
import com.pdfanalyzer.core.ml.scoring.StructuralScorePredictor;
import com.pdfanalyzer.core.semantic.model.SemanticCell;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 스펙트럼 점수 주입기
 * DJL 통합: 문서 분류 + 점수 예측 (ML 우선, 규칙 기반 폴백)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpectralScoreInjector {

    private final FFTEngine fftEngine;
    private final ResonanceFilterRegistry filterRegistry;
    private final DocumentClassifier documentClassifier;
    private final StructuralScorePredictor scorePredictor;

    @Value("${djl.score.hybrid.weight:0.7}")
    private double mlWeight; // ML 가중치 (0.7 = ML 70%, 규칙 30%)

    public List<SemanticCell> injectSpectralScores(List<SemanticCell> cells, DocumentType documentType) {
        log.info("스펙트럼 점수 주입: {} 셀, 타입: {}", cells.size(), documentType);

        if (cells.isEmpty()) return cells;

        ResonanceFilter filter = filterRegistry.getFilter(documentType);
        double[] resonances = fftEngine.analyzeResonance(cells, filter.getCoefficients());

        for (int i = 0; i < Math.min(cells.size(), resonances.length); i++) {
            SemanticCell cell = cells.get(i);
            double resonance = Math.abs(resonances[i]);
            cell.setResonanceIntensity(resonance);

            // ML 기반 점수 예측 (가능하면 하이브리드)
            double score = calculateScoreWithML(cell, resonance);
            cell.setStructuralScore(score);
        }

        log.info("점수 주입 완료");
        return cells;
    }

    public List<SemanticCell> injectSpectralScoresAutoDetect(List<SemanticCell> cells) {
        DocumentType detectedType = detectDocumentTypeWithML(cells);
        return injectSpectralScores(cells, detectedType);
    }

    /**
     * ML 기반 문서 타입 감지 (DJL 우선, 규칙 기반 폴백)
     */
    public DocumentType detectDocumentTypeWithML(List<SemanticCell> cells) {
        // DJL 분류기 시도
        if (documentClassifier.isAvailable()) {
            DocumentClassifier.ClassificationResult result = documentClassifier.predict(cells);

            if (documentClassifier.isConfident(result)) {
                log.info("✅ DJL 문서 분류 사용: {} (신뢰도: {:.2f})",
                        result.getDocumentType(), result.getConfidence());
                return result.getDocumentType();
            } else if (result != null) {
                log.warn("⚠️ DJL 신뢰도 낮음 ({:.2f}), 규칙 기반 폴백",
                        result.getConfidence());
            }
        }

        // 폴백: 규칙 기반 분류
        log.info("📊 규칙 기반 문서 분류 사용");
        return detectDocumentType(cells);
    }

    /**
     * ML 기반 점수 계산 (하이브리드 또는 규칙 기반 폴백)
     */
    private double calculateScoreWithML(SemanticCell cell, double resonance) {
        if (scorePredictor.isAvailable()) {
            // 하이브리드: ML + 규칙 앙상블
            double hybridScore = scorePredictor.calculateHybridScore(cell, resonance, mlWeight);
            log.debug("✅ 하이브리드 점수 사용: {:.3f} (ML weight: {:.1f})", hybridScore, mlWeight);
            return hybridScore;
        }

        // 폴백: 규칙 기반만
        return calculateScore(cell, resonance);
    }

    /**
     * 규칙 기반 점수 계산 (폴백용)
     */
    private double calculateScore(SemanticCell cell, double resonance) {
        double base = cell.getImportance();
        double normalized = Math.min(1.0, resonance / 10.0);
        double score = (base * 0.4) + (normalized * 0.6);
        return cell.isHeader() ? Math.min(1.0, score * 1.2) : score;
    }

    private DocumentType detectDocumentType(List<SemanticCell> cells) {
        int headers = (int) cells.stream().filter(SemanticCell::isHeader).count();
        int lists = (int) cells.stream().filter(c -> c.getType().toString().contains("LIST")).count();
        int paragraphs = (int) cells.stream().filter(c -> c.getType().toString().contains("PARAGRAPH")).count();

        if (paragraphs > cells.size() * 0.6 && headers > 3) return DocumentType.RESEARCH_PAPER;
        if (lists > cells.size() * 0.3) return DocumentType.CONTRACT;
        if (headers > cells.size() * 0.3) return DocumentType.PRESENTATION;
        if (headers > 2) return DocumentType.REPORT;
        return DocumentType.GENERAL;
    }
}
