package com.pdfanalyzer.core.integration.formatter;

import com.pdfanalyzer.core.analysis.validation.StructureValidator;
import com.pdfanalyzer.core.integration.llm.LLMAdapter;
import com.pdfanalyzer.core.semantic.model.SemanticCell;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ResultFormatter {

    public Map<String, Object> formatResult(
            String fileName,
            List<SemanticCell> cells,
            String summary,
            List<String> keywords,
            Map<String, List<LLMAdapter.KeywordLocation>> keywordLocations,
            StructureValidator.ValidationResult validation) {

        Map<String, Object> result = new HashMap<>();

        // 분석 ID 생성 (타임스탬프 기반)
        String analysisId = String.valueOf(System.currentTimeMillis());
        result.put("analysisId", analysisId);
        result.put("fileName", fileName);
        result.put("totalCells", cells.size());
        result.put("summary", summary);
        result.put("keywords", keywords);
        result.put("keywordLocations", keywordLocations);
        result.put("statistics", formatStatistics(cells));
        result.put("validation", formatValidation(validation));
        result.put("keySections", formatKeySections(cells));
        result.put("success", true);

        return result;
    }

    private Map<String, Object> formatStatistics(List<SemanticCell> cells) {
        Map<String, Object> stats = new HashMap<>();

        Map<String, Long> types = cells.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getType().toString(),
                        Collectors.counting()
                ));
        stats.put("typeDistribution", types);

        double avgScore = cells.stream()
                .mapToDouble(SemanticCell::getStructuralScore)
                .average()
                .orElse(0.0);
        stats.put("avgStructuralScore", avgScore);

        double maxScore = cells.stream()
                .mapToDouble(SemanticCell::getStructuralScore)
                .max()
                .orElse(0.0);
        stats.put("maxStructuralScore", maxScore);

        double avgResonance = cells.stream()
                .mapToDouble(SemanticCell::getResonanceIntensity)
                .average()
                .orElse(0.0);
        stats.put("avgResonance", avgResonance);

        stats.put("headerCount", cells.stream().filter(SemanticCell::isHeader).count());

        return stats;
    }

    private Map<String, Object> formatValidation(StructureValidator.ValidationResult validation) {
        Map<String, Object> val = new HashMap<>();
        val.put("isValid", validation.isValid());
        val.put("errors", validation.getErrors());
        val.put("warnings", validation.getWarnings());
        return val;
    }

    private List<Map<String, Object>> formatKeySections(List<SemanticCell> cells) {
        return cells.stream()
                .filter(c -> c.getStructuralScore() > 0.7)
                .limit(10)
                .map(cell -> {
                    Map<String, Object> section = new HashMap<>();
                    section.put("type", cell.getType().toString());
                    section.put("content", cell.getContent());
                    section.put("score", cell.getStructuralScore());
                    return section;
                })
                .collect(Collectors.toList());
    }
}
