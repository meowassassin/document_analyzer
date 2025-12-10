package com.pdfanalyzer.storage.repository;

import com.pdfanalyzer.storage.entity.DocumentMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 문서 메타데이터 리포지토리
 */
@Repository
public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadataEntity, Long> {

    /**
     * 파일 해시로 검색 (중복 체크)
     */
    Optional<DocumentMetadataEntity> findByFileHash(String fileHash);

    /**
     * 파일명으로 검색
     */
    List<DocumentMetadataEntity> findByFileNameContaining(String fileName);

    /**
     * 작성자로 검색
     */
    List<DocumentMetadataEntity> findByAuthorContaining(String author);

    /**
     * 업로드한 사용자로 검색
     */
    List<DocumentMetadataEntity> findByUploadedBy(String uploadedBy);

    /**
     * 최근 업로드 문서 조회
     */
    List<DocumentMetadataEntity> findTop20ByOrderByUploadedAtDesc();
}
