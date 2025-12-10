package com.pdfanalyzer.core.service;

import com.pdfanalyzer.core.analysis.scoring.SpectralScoreInjector;
import com.pdfanalyzer.core.analysis.validation.StructureValidator;
import com.pdfanalyzer.core.document.extractor.PDFExtractor;
import com.pdfanalyzer.core.document.model.DocumentMetadata;
import com.pdfanalyzer.core.frequency.filter.DocumentType;
import com.pdfanalyzer.core.integration.formatter.ResultFormatter;
import com.pdfanalyzer.core.integration.llm.LLMAdapter;
import com.pdfanalyzer.core.model.AnalysisResult;
import com.pdfanalyzer.core.semantic.builder.CellBuilder;
import com.pdfanalyzer.core.semantic.model.SemanticCell;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

/**
 * 문서 분석 파이프라인 서비스
 * 5계층 아키텍처 통합
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentAnalysisService {

    private final PDFExtractor pdfExtractor;
    private final CellBuilder cellBuilder;
    private final SpectralScoreInjector scoreInjector;
    private final StructureValidator validator;
    private final LLMAdapter llmAdapter;
    private final ResultFormatter resultFormatter;

    /**
     * 전체 파이프라인 실행
     */
    public AnalysisResult analyze(MultipartFile file, DocumentType documentType) throws IOException {
        log.info("=== 분석 시작: {} ===", file.getOriginalFilename());
        long start = System.currentTimeMillis();

        // 임시 파일 생성
        Path tempFile = Files.createTempFile("pdf-", ".pdf");
        Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

        try {
            return analyzePDF(tempFile.toFile(), documentType);
        } finally {
            Files.deleteIfExists(tempFile);
            log.info("=== 분석 완료: {}ms ===", System.currentTimeMillis() - start);
        }
    }

    /**
     * PDF 파일 분석
     */
    public AnalysisResult analyzePDF(File pdfFile, DocumentType documentType) throws IOException {
        AnalysisResult result = new AnalysisResult();

        try {
            // Step 1: PDF 텍스트 추출
            log.info("Step 1: PDF 추출");
            DocumentMetadata metadata = pdfExtractor.extractMetadata(pdfFile);
            List<String> pages = pdfExtractor.extractTextByPages(pdfFile);
            result.setMetadata(metadata);

            // Step 2: 셀 생성
            log.info("Step 2: 셀 생성");
            List<SemanticCell> cells = cellBuilder.buildCellsFromPages(pages);
            result.setCells(cells);

            // Step 3: FFT 분석 및 점수 주입
            log.info("Step 3: FFT 분석");
            if (documentType != null) {
                scoreInjector.injectSpectralScores(cells, documentType);
            } else {
                scoreInjector.injectSpectralScoresAutoDetect(cells);
            }

            // Step 4: 구조 검증
            log.info("Step 4: 검증");
            StructureValidator.ValidationResult validation = validator.validate(cells);
            result.setValidationResult(validation);

            // Step 5: LLM 요약 및 키워드 생성 (단일 API 호출)
            log.info("Step 5: 요약 및 키워드 생성");
            LLMAdapter.SummaryAndKeywords summaryAndKeywords = llmAdapter.generateSummaryAndKeywords(cells);
            result.setSummary(summaryAndKeywords.summary);
            result.setKeywords(summaryAndKeywords.keywords);

            // Step 6: 결과 포맷팅
            log.info("Step 6: 포맷팅");
            String fileName = pdfFile.getName();
            Map<String, Object> formatted = resultFormatter.formatResult(
                    fileName, cells, summaryAndKeywords.summary, summaryAndKeywords.keywords,
                    summaryAndKeywords.keywordLocations, validation);
            result.setFormattedResult(formatted);

            result.setSuccess(true);
            return result;

        } catch (Exception e) {
            log.error("분석 중 오류", e);
            result.setSuccess(false);
            result.setError(e.getMessage());
            return result;
        }
    }

    /**
     * 빠른 분석 (LLM 제외)
     */
    public AnalysisResult quickAnalyze(MultipartFile file) throws IOException {
        Path tempFile = Files.createTempFile("pdf-", ".pdf");
        Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

        try {
            AnalysisResult result = new AnalysisResult();

            DocumentMetadata metadata = pdfExtractor.extractMetadata(tempFile.toFile());
            List<String> pages = pdfExtractor.extractTextByPages(tempFile.toFile());
            List<SemanticCell> cells = cellBuilder.buildCellsFromPages(pages);

            scoreInjector.injectSpectralScoresAutoDetect(cells);
            StructureValidator.ValidationResult validation = validator.validate(cells);

            result.setMetadata(metadata);
            result.setCells(cells);
            result.setValidationResult(validation);
            result.setSummary("빠른 분석 완료: " + cells.size() + "개 셀 생성");
            result.setSuccess(true);

            return result;

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
