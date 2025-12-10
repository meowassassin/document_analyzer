package com.pdfanalyzer.core.model;

public class Cell {
    
    private int index;
    private String text;
    private int length; // 텍스트 길이(주파수 신호 사용 편의성)

    public Cell(int index, String text) {
        this.index = index;
        this.text = text;
        this.length = text != null ? text.length() : 0;
    }

    public int getIndex() {
        return index;
    }

    public String getText() {
        return text;
    }

    public int getLength() {
        return length;
    }
}
