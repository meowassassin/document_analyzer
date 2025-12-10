package com.pdfanalyzer.core.semantic.builder;

import com.pdfanalyzer.core.document.analyzer.LayoutAnalyzer;
import com.pdfanalyzer.core.document.model.BlockType;
import com.pdfanalyzer.core.semantic.embedding.TokenMixer;
import com.pdfanalyzer.core.semantic.model.SemanticCell;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 텍스트를 의미 단위 셀로 변환하는 빌더
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CellBuilder {

    private final TokenMixer tokenMixer;
    private final LayoutAnalyzer layoutAnalyzer;

    public List<SemanticCell> buildCells(List<String> paragraphs) {
        log.info("셀 생성 시작: {} 문단", paragraphs.size());

        List<SemanticCell> cells = new ArrayList<>();
        int currentPosition = 0;

        for (int i = 0; i < paragraphs.size(); i++) {
            String paragraph = paragraphs.get(i);
            SemanticCell cell = buildCell(paragraph, currentPosition, i);
            cells.add(cell);
            currentPosition += paragraph.length() + 2;
        }

        log.info("셀 생성 완료: {} 셀", cells.size());
        return cells;
    }

    public SemanticCell buildCell(String content, int position, int index) {
        BlockType blockType = layoutAnalyzer.inferBlockType(content);
        double[] embedding = tokenMixer.createSentenceEmbedding(content);
        SemanticCell.CellLayoutInfo layoutInfo = createLayoutInfo(content);

        SemanticCell cell = SemanticCell.builder()
                .id(UUID.randomUUID().toString())
                .type(blockType)
                .content(content)
                .position(position)
                .endPosition(position + content.length())
                .embeddingVector(embedding)
                .layoutInfo(layoutInfo)
                .structuralScore(0.0)
                .resonanceIntensity(0.0)
                .importance(calculateImportance(blockType, content))
                .build();

        log.debug("셀 생성: {} - {} ({})", cell.getId(), blockType, content.length());
        return cell;
    }

    public List<SemanticCell> buildCellsFromText(String fullText) {
        List<String> paragraphs = layoutAnalyzer.analyzeParagraphs(fullText);
        return buildCells(paragraphs);
    }

    public List<SemanticCell> buildCellsFromPages(List<String> pages) {
        log.info("페이지별 셀 생성 시작: {} 페이지", pages.size());

        List<SemanticCell> allCells = new ArrayList<>();
        int globalPosition = 0;

        for (int pageNum = 0; pageNum < pages.size(); pageNum++) {
            String pageText = pages.get(pageNum);
            List<String> paragraphs = layoutAnalyzer.analyzeParagraphs(pageText);

            for (String paragraph : paragraphs) {
                SemanticCell cell = buildCell(paragraph, globalPosition, allCells.size());
                cell.setPageNumber(pageNum + 1);
                allCells.add(cell);
                globalPosition += paragraph.length() + 2;
            }
        }

        log.info("페이지별 셀 생성 완료: 총 {} 셀", allCells.size());
        return allCells;
    }

    private SemanticCell.CellLayoutInfo createLayoutInfo(String content) {
        String[] lines = content.split("\\n");
        String[] words = content.split("\\s+");

        int indentLevel = 0;
        if (lines.length > 0 && lines[0].length() > 0) {
            for (char c : lines[0].toCharArray()) {
                if (c == ' ' || c == '\t') {
                    indentLevel++;
                } else {
                    break;
                }
            }
        }

        return SemanticCell.CellLayoutInfo.builder()
                .lineCount(lines.length)
                .wordCount(words.length)
                .charCount(content.length())
                .indentLevel(indentLevel)
                .relativeFontSize(estimateFontSize(content))
                .build();
    }

    private double calculateImportance(BlockType blockType, String content) {
        double importance = 0.5;

        switch (blockType) {
            case TITLE: importance = 1.0; break;
            case SECTION_HEADER: importance = 0.9; break;
            case SUBSECTION_HEADER: importance = 0.8; break;
            case PARAGRAPH: importance = 0.5; break;
            case LIST_ITEM: importance = 0.6; break;
            case TABLE: importance = 0.7; break;
            default: importance = 0.5;
        }

        if (content.length() > 500) {
            importance += 0.1;
        }

        return Math.min(1.0, importance);
    }

    private double estimateFontSize(String content) {
        long uppercaseCount = content.chars().filter(Character::isUpperCase).count();
        double uppercaseRatio = (double) uppercaseCount / content.length();

        if (uppercaseRatio > 0.5) {
            return 1.5;
        } else if (content.length() < 50) {
            return 1.2;
        } else {
            return 1.0;
        }
    }
}
