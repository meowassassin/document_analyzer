package com.pdfanalyzer.core.frequency.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 문서 구조 타입별 주파수 필터
 * P[k]: 문서 구조 필터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResonanceFilter {
    private String name;
    private DocumentType documentType;
    private double[] coefficients;
    private String description;
}
