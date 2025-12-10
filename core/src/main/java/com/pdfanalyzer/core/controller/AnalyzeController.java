package com.pdfanalyzer.core.controller;

import com.pdfanalyzer.core.frequency.filter.DocumentType;
import com.pdfanalyzer.core.model.AnalysisResult;
import com.pdfanalyzer.core.service.DocumentAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 문서 분석 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/analyze")
@RequiredArgsConstructor
public class AnalyzeController {

    private final DocumentAnalysisService analysisService;
    private final com.pdfanalyzer.core.service.AnalysisCacheService cacheService;

    /**
     * 전체 분석 (LLM 포함)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyze(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "documentType", required = false) String documentType) {

        log.info("분석 요청: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "파일이 비어있습니다"));
        }

        try {
            DocumentType type = parseDocumentType(documentType);
            AnalysisResult result = analysisService.analyze(file, type);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result.getFormattedResult());
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", result.getError()));
            }

        } catch (Exception e) {
            log.error("분석 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", e.getMessage(),
                        "message", e.getMessage(),
                        "success", false
                    ));
        }
    }

    /**
     * 빠른 분석 (LLM 제외)
     */
    @PostMapping(value = "/quick", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> quickAnalyze(@RequestPart("file") MultipartFile file) {
        log.info("빠른 분석: {}", file.getOriginalFilename());

        try {
            AnalysisResult result = analysisService.quickAnalyze(file);

            return ResponseEntity.ok(Map.of(
                    "success", result.isSuccess(),
                    "cellCount", result.getCells().size(),
                    "summary", result.getSummary(),
                    "validation", result.getValidationResult()
            ));

        } catch (Exception e) {
            log.error("빠른 분석 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", e.getMessage(),
                        "message", e.getMessage(),
                        "success", false
                    ));
        }
    }

    /**
     * 문서 타입 목록
     */
    @GetMapping("/document-types")
    public ResponseEntity<?> getDocumentTypes() {
        return ResponseEntity.ok(Map.of(
                "types", DocumentType.values(),
                "description", Map.of(
                        "RESEARCH_PAPER", "연구 논문",
                        "REPORT", "보고서",
                        "CONTRACT", "계약서",
                        "PRESENTATION", "발표 자료",
                        "MANUAL", "매뉴얼",
                        "GENERAL", "일반 문서"
                )
        ));
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Cell-Frequency Parser",
                "version", "1.0.0"
        ));
    }

    /**
     * 캐시 통계 조회
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<?> getCacheStats() {
        return ResponseEntity.ok(cacheService.getStats());
    }

    /**
     * 캐시 초기화
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<?> clearCache() {
        cacheService.clear();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "캐시가 초기화되었습니다"
        ));
    }

    private DocumentType parseDocumentType(String type) {
        if (type == null || type.isEmpty()) {
            return null;
        }
        try {
            return DocumentType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("유효하지 않은 문서 타입: {}", type);
            return null;
        }
    }
}
