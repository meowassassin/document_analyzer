package com.pdfanalyzer.core.analysis.validation;

import com.pdfanalyzer.core.semantic.model.SemanticCell;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class StructureValidator {

    public ValidationResult validate(List<SemanticCell> cells) {
        log.info("구조 검증 시작: {} 셀", cells.size());

        ValidationResult result = new ValidationResult();
        result.setTotalCells(cells.size());

        validateIntegrity(cells, result);
        validateConsistency(cells, result);

        result.setValid(result.getErrors().isEmpty());
        log.info("검증 완료: {} 오류, {} 경고", result.getErrors().size(), result.getWarnings().size());

        return result;
    }

    private void validateIntegrity(List<SemanticCell> cells, ValidationResult result) {
        for (int i = 0; i < cells.size(); i++) {
            SemanticCell cell = cells.get(i);
            if (cell.getId() == null || cell.getId().isEmpty()) {
                result.addError(i, "셀 ID 누락");
            }
            if (cell.getContent() == null || cell.getContent().isEmpty()) {
                result.addWarning(i, "셀 내용 비어있음");
            }
            if (cell.getStructuralScore() < 0 || cell.getStructuralScore() > 1) {
                result.addError(i, "구조 점수 범위 오류: " + cell.getStructuralScore());
            }
        }
    }

    private void validateConsistency(List<SemanticCell> cells, ValidationResult result) {
        SemanticCell prev = null;
        for (int i = 0; i < cells.size(); i++) {
            SemanticCell cell = cells.get(i);
            if (prev != null && cell.getPosition() < prev.getEndPosition()) {
                result.addWarning(i, "위치 겹침");
            }
            prev = cell;
        }
    }

    @Data
    public static class ValidationResult {
        private boolean valid;
        private int totalCells;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        public void addError(int index, String msg) {
            errors.add(String.format("[%d] %s", index, msg));
        }

        public void addWarning(int index, String msg) {
            warnings.add(String.format("[%d] %s", index, msg));
        }
    }
}
