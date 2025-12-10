package com.pdfanalyzer.core.document.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 문서의 구조적 요소들을 담는 클래스
 */
@Data
public class LayoutStructure {
    private List<StructureElement> sectionHeaders = new ArrayList<>();
    private List<StructureElement> subsectionHeaders = new ArrayList<>();
    private List<StructureElement> listItems = new ArrayList<>();

    public void addSectionHeader(String text, int position) {
        sectionHeaders.add(new StructureElement(text, position, ElementType.SECTION_HEADER));
    }

    public void addSubsectionHeader(String text, int position) {
        subsectionHeaders.add(new StructureElement(text, position, ElementType.SUBSECTION_HEADER));
    }

    public void addListItem(String text, int position) {
        listItems.add(new StructureElement(text, position, ElementType.LIST_ITEM));
    }

    @Data
    public static class StructureElement {
        private final String text;
        private final int position;
        private final ElementType type;
    }

    public enum ElementType {
        SECTION_HEADER,
        SUBSECTION_HEADER,
        LIST_ITEM
    }
}
