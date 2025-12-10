package com.pdfanalyzer.storage.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * ML 학습 데이터 저장 엔티티
 */
@Entity
@Table(name = "training_data", indexes = {
    @Index(name = "idx_label", columnList = "label"),
    @Index(name = "idx_source", columnList = "source"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long analysisResultId;  // 원본 분석 결과 ID

    @Column(columnDefinition = "TEXT", nullable = false)
    private String featureVector;  // JSON 형식의 특징 벡터

    @Column(nullable = false, length = 100)
    private String label;  // 정답 라벨

    @Column(nullable = false)
    private Double confidence;  // 라벨 신뢰도 (0~1)

    @Column(nullable = false, length = 50)
    private String source;  // 데이터 출처: rule_based, user_feedback, synthetic

    @Column(length = 100)
    private String modelType;  // classifier, score_predictor, filter_optimizer

    @Column(columnDefinition = "TEXT")
    private String metadata;  // 추가 메타데이터 (JSON)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(length = 100)
    private String userId;  // 데이터 제공자
}
