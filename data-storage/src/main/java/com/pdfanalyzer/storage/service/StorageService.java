package com.pdfanalyzer.storage.service;

import com.pdfanalyzer.storage.entity.AnalysisResultEntity;
import com.pdfanalyzer.storage.entity.DocumentMetadataEntity;
import com.pdfanalyzer.storage.repository.AnalysisResultRepository;
import com.pdfanalyzer.storage.repository.DocumentMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 저장 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final AnalysisResultRepository analysisResultRepository;
    private final DocumentMetadataRepository documentMetadataRepository;

    /**
     * 분석 결과 저장
     */
    @Transactional
    public AnalysisResultEntity saveAnalysisResult(AnalysisResultEntity entity) {
        log.info("분석 결과 저장: {}", entity.getFileName());
        return analysisResultRepository.save(entity);
    }

    /**
     * 문서 메타데이터 저장
     */
    @Transactional
    public DocumentMetadataEntity saveDocumentMetadata(DocumentMetadataEntity entity) {
        log.info("문서 메타데이터 저장: {}", entity.getFileName());
        return documentMetadataRepository.save(entity);
    }

    /**
     * ID로 분석 결과 조회
     */
    public Optional<AnalysisResultEntity> getAnalysisResultById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return analysisResultRepository.findById(id);
    }

    /**
     * 파일명으로 분석 결과 검색
     */
    public List<AnalysisResultEntity> searchByFileName(String fileName) {
        return analysisResultRepository.findByFileNameContaining(fileName);
    }

    /**
     * 문서 타입으로 분석 결과 검색
     */
    public List<AnalysisResultEntity> searchByDocumentType(String documentType) {
        return analysisResultRepository.findByDocumentType(documentType);
    }

    /**
     * 사용자별 분석 결과 조회
     */
    public List<AnalysisResultEntity> getResultsByUser(String userId) {
        return analysisResultRepository.findByUserId(userId);
    }

    /**
     * 최근 분석 결과 조회
     */
    public List<AnalysisResultEntity> getRecentResults() {
        return analysisResultRepository.findTop10ByOrderByCreatedAtDesc();
    }

    /**
     * 기간별 분석 결과 조회
     */
    public List<AnalysisResultEntity> getResultsByDateRange(LocalDateTime start, LocalDateTime end) {
        return analysisResultRepository.findByCreatedAtBetween(start, end);
    }

    /**
     * 파일 해시로 중복 체크
     */
    public Optional<DocumentMetadataEntity> findByFileHash(String fileHash) {
        return documentMetadataRepository.findByFileHash(fileHash);
    }

    /**
     * 최근 업로드 문서 조회
     */
    public List<DocumentMetadataEntity> getRecentDocuments() {
        return documentMetadataRepository.findTop20ByOrderByUploadedAtDesc();
    }

    /**
     * 분석 결과 삭제
     */
    @Transactional
    public void deleteAnalysisResult(Long id) {
        if (id == null) {
            log.warn("삭제할 ID가 null입니다");
            return;
        }
        log.info("분석 결과 삭제: {}", id);
        analysisResultRepository.deleteById(id);
    }

    /**
     * 모든 분석 결과 조회
     */
    public List<AnalysisResultEntity> getAllResults() {
        return analysisResultRepository.findAll();
    }

    /**
     * 고품질 결과 조회 (구조 점수 기준)
     */
    public List<AnalysisResultEntity> getHighQualityResults(Double minScore) {
        return analysisResultRepository.findByMinStructuralScore(minScore);
    }
}
