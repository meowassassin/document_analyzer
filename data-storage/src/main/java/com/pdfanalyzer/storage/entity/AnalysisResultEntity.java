package com.pdfanalyzer.storage.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 분석 결과 저장 엔티티
 */
@Entity
@Table(name = "analysis_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String fileName;

    @Column(nullable = false, length = 50)
    private String documentType;

    @Column(nullable = false)
    private Integer totalPages;

    @Column(nullable = false)
    private Integer totalCells;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String keywords;

    @Column(nullable = false)
    private Double avgStructuralScore;

    @Column(nullable = false)
    private Double avgResonanceIntensity;

    @Column(nullable = false)
    private Double peakFrequency;

    @Column(nullable = false)
    private Double dominantAmplitude;

    @Column(columnDefinition = "TEXT")
    private String topCells;

    @Column(columnDefinition = "TEXT")
    private String validationWarnings;

    @Column(nullable = false)
    private Long processingTimeMs;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 100)
    private String userId;

    @Column(length = 50)
    private String status;
}
