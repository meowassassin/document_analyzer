package com.pdfanalyzer.core.document.model;

import lombok.Data;

/**
 * 문서 레이아웃의 통계 정보
 */
@Data
public class LayoutInfo {
    private int lineCount;
    private int wordCount;
    private int charCount;
    private double averageLineLength;
}
