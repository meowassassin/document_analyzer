package com.pdfanalyzer.core.document.model;

/**
 * 텍스트 블록의 타입을 정의하는 열거형
 */
public enum BlockType {
    TITLE,              // 제목
    SECTION_HEADER,     // 섹션 헤더
    SUBSECTION_HEADER,  // 하위 섹션 헤더
    PARAGRAPH,          // 일반 문단
    LIST_ITEM,          // 리스트 항목
    TABLE,              // 표
    CAPTION,            // 캡션
    FOOTER,             // 푸터
    HEADER              // 헤더
}
