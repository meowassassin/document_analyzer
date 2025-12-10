package com.pdfanalyzer.core.model;

import com.pdfanalyzer.core.analysis.validation.StructureValidator;
import com.pdfanalyzer.core.document.model.DocumentMetadata;
import com.pdfanalyzer.core.semantic.model.SemanticCell;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 문서 분석 결과 DTO
 */
@Data
public class AnalysisResult {
    private boolean success;
    private String error;
    private DocumentMetadata metadata;
    private List<SemanticCell> cells;
    private String summary;
    private List<String> keywords;
    private StructureValidator.ValidationResult validationResult;
    private Map<String, Object> formattedResult;
}
