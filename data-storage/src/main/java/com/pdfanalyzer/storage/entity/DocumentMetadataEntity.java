package com.pdfanalyzer.storage.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 문서 메타데이터 저장 엔티티
 */
@Entity
@Table(name = "document_metadata")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetadataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String fileName;

    @Column(nullable = false)
    private Long fileSize;

    @Column(length = 100)
    private String mimeType;

    @Column(length = 64)
    private String fileHash;

    @Column(columnDefinition = "TEXT")
    private String filePath;

    @Column(nullable = false)
    private Integer pageCount;

    @Column(length = 500)
    private String author;

    @Column(length = 500)
    private String title;

    @Column(length = 500)
    private String subject;

    @Column
    private LocalDateTime creationDate;

    @Column
    private LocalDateTime modificationDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(length = 100)
    private String uploadedBy;
}
