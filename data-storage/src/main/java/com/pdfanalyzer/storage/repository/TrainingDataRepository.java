package com.pdfanalyzer.storage.repository;

import com.pdfanalyzer.storage.entity.TrainingDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 학습 데이터 리포지토리
 */
@Repository
public interface TrainingDataRepository extends JpaRepository<TrainingDataEntity, Long> {

    // 라벨별 조회
    List<TrainingDataEntity> findByLabel(String label);

    // 데이터 출처별 조회
    List<TrainingDataEntity> findBySource(String source);

    // 모델 타입별 조회
    List<TrainingDataEntity> findByModelType(String modelType);

    // 기간별 조회
    List<TrainingDataEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // 신뢰도 이상 조회
    @Query("SELECT t FROM TrainingDataEntity t WHERE t.confidence >= ?1")
    List<TrainingDataEntity> findByMinConfidence(Double minConfidence);

    // 최근 데이터 조회
    List<TrainingDataEntity> findTop1000ByOrderByCreatedAtDesc();

    // 라벨 분포 조회
    @Query("SELECT t.label, COUNT(t) FROM TrainingDataEntity t GROUP BY t.label")
    List<Object[]> getLabelDistribution();

    // 모델별 데이터 개수
    @Query("SELECT t.modelType, COUNT(t) FROM TrainingDataEntity t GROUP BY t.modelType")
    List<Object[]> getModelTypeDistribution();
}
