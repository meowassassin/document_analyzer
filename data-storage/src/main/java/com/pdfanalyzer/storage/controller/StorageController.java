package com.pdfanalyzer.storage.controller;

import com.pdfanalyzer.storage.entity.AnalysisResultEntity;
import com.pdfanalyzer.storage.entity.DocumentMetadataEntity;
import com.pdfanalyzer.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 저장소 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    /**
     * 분석 결과 저장
     */
    @PostMapping("/results")
    public ResponseEntity<?> saveResult(@RequestBody AnalysisResultEntity entity) {
        log.info("분석 결과 저장 요청: {}", entity.getFileName());
        try {
            AnalysisResultEntity saved = storageService.saveAnalysisResult(entity);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("저장 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 문서 메타데이터 저장
     */
    @PostMapping("/metadata")
    public ResponseEntity<?> saveMetadata(@RequestBody DocumentMetadataEntity entity) {
        log.info("메타데이터 저장 요청: {}", entity.getFileName());
        try {
            DocumentMetadataEntity saved = storageService.saveDocumentMetadata(entity);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("저장 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ID로 분석 결과 조회
     */
    @GetMapping("/results/{id}")
    public ResponseEntity<?> getResult(@PathVariable Long id) {
        return storageService.getAnalysisResultById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 모든 분석 결과 조회
     */
    @GetMapping("/results")
    public ResponseEntity<List<AnalysisResultEntity>> getAllResults() {
        return ResponseEntity.ok(storageService.getAllResults());
    }

    /**
     * 파일명으로 검색
     */
    @GetMapping("/results/search")
    public ResponseEntity<List<AnalysisResultEntity>> searchByFileName(
            @RequestParam String fileName) {
        return ResponseEntity.ok(storageService.searchByFileName(fileName));
    }

    /**
     * 문서 타입으로 검색
     */
    @GetMapping("/results/type/{documentType}")
    public ResponseEntity<List<AnalysisResultEntity>> getByDocumentType(
            @PathVariable String documentType) {
        return ResponseEntity.ok(storageService.searchByDocumentType(documentType));
    }

    /**
     * 사용자별 결과 조회
     */
    @GetMapping("/results/user/{userId}")
    public ResponseEntity<List<AnalysisResultEntity>> getByUser(
            @PathVariable String userId) {
        return ResponseEntity.ok(storageService.getResultsByUser(userId));
    }

    /**
     * 최근 결과 조회
     */
    @GetMapping("/results/recent")
    public ResponseEntity<List<AnalysisResultEntity>> getRecentResults() {
        return ResponseEntity.ok(storageService.getRecentResults());
    }

    /**
     * 기간별 결과 조회
     */
    @GetMapping("/results/date-range")
    public ResponseEntity<List<AnalysisResultEntity>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(storageService.getResultsByDateRange(start, end));
    }

    /**
     * 고품질 결과 조회
     */
    @GetMapping("/results/high-quality")
    public ResponseEntity<List<AnalysisResultEntity>> getHighQualityResults(
            @RequestParam(defaultValue = "0.7") Double minScore) {
        return ResponseEntity.ok(storageService.getHighQualityResults(minScore));
    }

    /**
     * 분석 결과 삭제
     */
    @DeleteMapping("/results/{id}")
    public ResponseEntity<?> deleteResult(@PathVariable Long id) {
        log.info("분석 결과 삭제 요청: {}", id);
        try {
            storageService.deleteAnalysisResult(id);
            return ResponseEntity.ok(Map.of("message", "삭제 완료"));
        } catch (Exception e) {
            log.error("삭제 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 파일 중복 체크
     */
    @GetMapping("/metadata/check-duplicate/{fileHash}")
    public ResponseEntity<?> checkDuplicate(@PathVariable String fileHash) {
        return storageService.findByFileHash(fileHash)
                .map(metadata -> ResponseEntity.ok(Map.of(
                        "duplicate", true,
                        "metadata", metadata
                )))
                .orElse(ResponseEntity.ok(Map.of("duplicate", false)));
    }

    /**
     * 최근 문서 조회
     */
    @GetMapping("/metadata/recent")
    public ResponseEntity<List<DocumentMetadataEntity>> getRecentDocuments() {
        return ResponseEntity.ok(storageService.getRecentDocuments());
    }

    /**
     * Health Check
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Data Storage Service",
                "version", "1.0.0"
        ));
    }
}
