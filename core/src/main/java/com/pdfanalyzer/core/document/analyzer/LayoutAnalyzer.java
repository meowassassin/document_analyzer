package com.pdfanalyzer.core.document.analyzer;

import com.pdfanalyzer.core.document.model.BlockType;
import com.pdfanalyzer.core.document.model.LayoutInfo;
import com.pdfanalyzer.core.document.model.LayoutStructure;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 문서의 레이아웃 및 구조적 요소를 분석하는 컴포넌트
 */
@Slf4j
@Component
public class LayoutAnalyzer {

    private static final Pattern SECTION_HEADER_PATTERN = Pattern.compile("^(\\d+\\.\\s+.+)$", Pattern.MULTILINE);
    private static final Pattern SUBSECTION_HEADER_PATTERN = Pattern.compile("^(\\d+\\.\\d+\\.?\\s+.+)$", Pattern.MULTILINE);
    private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("^[•\\-\\*]\\s+.+$", Pattern.MULTILINE);

    /**
     * 텍스트를 분석하여 문단 단위로 분리
     */
    public List<String> analyzeParagraphs(String text) {
        log.debug("문단 분석 시작");

        List<String> paragraphs = new ArrayList<>();
        String[] splits = text.split("\\n\\s*\\n");

        for (String para : splits) {
            String trimmed = para.trim();
            if (!trimmed.isEmpty()) {
                paragraphs.add(trimmed);
            }
        }

        log.info("총 {} 개의 문단 발견", paragraphs.size());
        return paragraphs;
    }

    /**
     * 텍스트에서 구조적 요소를 분석
     */
    public LayoutStructure analyzeStructure(String text) {
        log.info("문서 구조 분석 시작");

        LayoutStructure structure = new LayoutStructure();

        Matcher sectionMatcher = SECTION_HEADER_PATTERN.matcher(text);
        while (sectionMatcher.find()) {
            structure.addSectionHeader(sectionMatcher.group(1), sectionMatcher.start());
        }

        Matcher subsectionMatcher = SUBSECTION_HEADER_PATTERN.matcher(text);
        while (subsectionMatcher.find()) {
            structure.addSubsectionHeader(subsectionMatcher.group(1), subsectionMatcher.start());
        }

        Matcher listMatcher = LIST_ITEM_PATTERN.matcher(text);
        while (listMatcher.find()) {
            structure.addListItem(listMatcher.group(0), listMatcher.start());
        }

        log.info("구조 분석 완료: 섹션 {}, 하위섹션 {}, 리스트 {}",
                structure.getSectionHeaders().size(),
                structure.getSubsectionHeaders().size(),
                structure.getListItems().size());

        return structure;
    }

    /**
     * 텍스트의 레이아웃 정보를 분석
     */
    public LayoutInfo analyzeLayoutInfo(String text) {
        LayoutInfo info = new LayoutInfo();

        info.setLineCount(text.split("\\n").length);
        info.setWordCount(text.split("\\s+").length);
        info.setCharCount(text.length());

        String[] lines = text.split("\\n");
        double avgLineLength = lines.length > 0 ? (double) text.length() / lines.length : 0;
        info.setAverageLineLength(avgLineLength);

        log.debug("레이아웃 정보: {} 줄, {} 단어, {} 문자",
                info.getLineCount(), info.getWordCount(), info.getCharCount());

        return info;
    }

    /**
     * 텍스트 블록의 타입을 추론
     */
    public BlockType inferBlockType(String text) {
        text = text.trim();

        if (SECTION_HEADER_PATTERN.matcher(text).matches()) {
            return BlockType.SECTION_HEADER;
        }

        if (SUBSECTION_HEADER_PATTERN.matcher(text).matches()) {
            return BlockType.SUBSECTION_HEADER;
        }

        if (LIST_ITEM_PATTERN.matcher(text).matches()) {
            return BlockType.LIST_ITEM;
        }

        if (text.contains("|") && text.split("\\|").length > 2) {
            return BlockType.TABLE;
        }

        if (text.length() < 100 && !text.contains(".")) {
            return BlockType.TITLE;
        }

        return BlockType.PARAGRAPH;
    }
}
