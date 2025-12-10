package com.pdfanalyzer.core.semantic.model;

import com.pdfanalyzer.core.document.model.BlockType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 문서의 의미 단위를 나타내는 셀 구조
 * Cell = { id, type, position, embedding_vector, layout_info }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticCell {
    private String id;
    private BlockType type;
    private String content;
    private int position;
    private int endPosition;
    private Integer pageNumber;
    private double[] embeddingVector;
    private CellLayoutInfo layoutInfo;
    private double structuralScore;
    private double resonanceIntensity;
    private String parentCellId;
    private double importance;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CellLayoutInfo {
        private int lineCount;
        private int wordCount;
        private int charCount;
        private int indentLevel;
        private double relativeFontSize;
    }

    public boolean isHeader() {
        return type == BlockType.TITLE
                || type == BlockType.SECTION_HEADER
                || type == BlockType.SUBSECTION_HEADER;
    }

    public int getLength() {
        return content != null ? content.length() : 0;
    }
}
