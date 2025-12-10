package com.pdfanalyzer.core.document.model;

import lombok.Data;

/**
 * PDF 문서의 메타데이터를 담는 DTO
 */
@Data
public class DocumentMetadata {
    private int pageCount;
    private String title;
    private String author;
    private String subject;
    private String creator;
    private String producer;
}
