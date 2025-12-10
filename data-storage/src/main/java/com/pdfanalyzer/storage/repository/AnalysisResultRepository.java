package com.pdfanalyzer.storage.repository;

import com.pdfanalyzer.storage.entity.AnalysisResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 분석 결과 리포지토리
 */
@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResultEntity, Long> {

    /**
     * 파일명으로 검색
     */
    List<AnalysisResultEntity> findByFileNameContaining(String fileName);

    /**
     * 문서 타입으로 검색
     */
    List<AnalysisResultEntity> findByDocumentType(String documentType);

    /**
     * 사용자 ID로 검색
     */
    List<AnalysisResultEntity> findByUserId(String userId);

    /**
     * 기간별 검색
     */
    List<AnalysisResultEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 최근 N개 결과 조회
     */
    List<AnalysisResultEntity> findTop10ByOrderByCreatedAtDesc();

    /**
     * 파일명과 사용자로 검색
     */
    Optional<AnalysisResultEntity> findFirstByFileNameAndUserIdOrderByCreatedAtDesc(String fileName, String userId);

    /**
     * 평균 점수 이상인 결과 조회
     */
    @Query("SELECT a FROM AnalysisResultEntity a WHERE a.avgStructuralScore >= :minScore ORDER BY a.avgStructuralScore DESC")
    List<AnalysisResultEntity> findByMinStructuralScore(Double minScore);
}
