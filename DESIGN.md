# 셀-주파수 기반 문서 분석기 - 상세 설계 문서

## 목차
1. [시스템 개요](#1-시스템-개요)
2. [5계층 아키텍처 상세](#2-5계층-아키텍처-상세)
3. [핵심 알고리즘](#3-핵심-알고리즘)
4. [데이터 모델](#4-데이터-모델)
5. [API 명세](#5-api-명세)
6. [성능 최적화 전략](#6-성능-최적화-전략)
7. [확장 가능성](#7-확장-가능성)

---

## 1. 시스템 개요

### 1.1 설계 철학
- **모듈화**: 5개의 독립된 계층으로 구성하여 유지보수성 향상
- **재사용성**: 각 계층은 독립적으로 테스트 및 교체 가능
- **확장성**: 새로운 문서 타입 및 필터를 쉽게 추가 가능
- **효율성**: O(N log N) 복잡도로 대용량 문서 처리

### 1.2 핵심 개념

#### Cell (셀)
문서의 최소 의미 단위로, 다음 정보를 포함:
- 내용 (content)
- 타입 (type): TITLE, SECTION_HEADER, PARAGRAPH 등
- 위치 정보 (position, endPosition)
- 임베딩 벡터 (embeddingVector)
- 구조 점수 (structuralScore)
- 공명 강도 (resonanceIntensity)

#### Resonance (공명)
FFT를 통해 계산된 셀의 구조적 중요도 지표

---

## 2. 5계층 아키텍처 상세

### Layer 1: Document Processing Layer

**목적**: PDF 문서에서 원시 텍스트와 레이아웃 정보 추출

#### 주요 컴포넌트

##### PDFExtractor
```java
public class PDFExtractor {
    // PDF에서 페이지별 텍스트 추출
    List<String> extractTextByPages(File pdfFile)

    // 전체 텍스트 추출
    String extractFullText(File pdfFile)

    // 메타데이터 추출 (제목, 저자, 페이지 수)
    DocumentMetadata extractMetadata(File pdfFile)
}
```

##### LayoutAnalyzer
```java
public class LayoutAnalyzer {
    // 문단 단위로 분리
    List<String> analyzeParagraphs(String text)

    // 구조적 요소 분석 (헤더, 리스트 등)
    LayoutStructure analyzeStructure(String text)

    // 블록 타입 추론
    BlockType inferBlockType(String text)
}
```

**입력**: PDF 파일
**출력**: 텍스트 문자열, 레이아웃 구조, 메타데이터

---

### Layer 2: Semantic Cell Layer

**목적**: 텍스트를 의미 단위 셀로 변환하고 임베딩 생성

#### 주요 컴포넌트

##### Cell (데이터 모델)
```java
@Data
public class Cell {
    String id;                    // 고유 ID
    BlockType type;               // 타입
    String content;               // 내용
    int position;                 // 시작 위치
    double[] embeddingVector;     // 임베딩
    double structuralScore;       // 구조 점수
    double resonanceIntensity;    // 공명 강도
    CellLayoutInfo layoutInfo;    // 레이아웃 정보
}
```

##### TokenMixer
```java
public class TokenMixer {
    // 토큰화
    double[][] tokenize(String text)

    // 토큰 믹싱: E'ᵢ = (Eᵢ₋₁ + Eᵢ + Eᵢ₊₁) / 3
    double[][] mixTokens(double[][] embeddings)

    // 문장 임베딩 생성
    double[] createSentenceEmbedding(String text)
}
```

**Token Mixing 알고리즘**:
- 인접 토큰의 임베딩을 평균화하여 문맥 정보 보존
- 의미 손실 최소화

##### CellBuilder
```java
public class CellBuilder {
    // 문단 → 셀 변환
    List<Cell> buildCells(List<String> paragraphs)

    // 전체 텍스트 → 셀 시퀀스
    List<Cell> buildCellsFromText(String fullText)

    // 페이지별 변환
    List<Cell> buildCellsFromPages(List<String> pages)
}
```

**입력**: 텍스트 문단 리스트
**출력**: Cell 객체 리스트

---

### Layer 3: Frequency Analysis Layer

**목적**: FFT를 사용하여 셀 시퀀스의 주파수 스펙트럼 분석

#### 주요 컴포넌트

##### FFTEngine
```java
public class FFTEngine {
    // 셀 시퀀스를 FFT로 변환
    FFTSpectrum transform(List<Cell> cells)

    // 역변환 (IFFT)
    double[] inverseTransform(FFTSpectrum spectrum)

    // 필터 적용
    FFTSpectrum applyFilter(FFTSpectrum spectrum, double[] filter)

    // 공명 분석: R = IFFT(X · P)
    double[] analyzeResonance(List<Cell> cells, double[] filter)
}
```

**FFT 변환 과정**:
1. 셀을 실수 신호로 변환 (중요도, 길이, 타입 등 반영)
2. JTransforms를 사용하여 FFT 수행
3. 복소수 스펙트럼 생성

##### ResonanceFilterRegistry
```java
public class ResonanceFilterRegistry {
    // 문서 타입별 필터 등록
    void registerFilter(ResonanceFilter filter)

    // 필터 조회
    ResonanceFilter getFilter(DocumentType type)
}
```

**문서 타입별 필터 특성**:
- **RESEARCH_PAPER**: 저주파 강조 (큰 섹션 구조)
- **REPORT**: 중간 주파수 강조
- **CONTRACT**: 반복 패턴 감지
- **PRESENTATION**: 고주파 강조 (슬라이드 단위)

**입력**: Cell 리스트
**출력**: 공명 강도 배열

---

### Layer 4: Parsing & Validation Layer

**목적**: FFT 결과를 셀에 주입하고 구조 검증

#### 주요 컴포넌트

##### SpectralScoreInjector
```java
public class SpectralScoreInjector {
    // 스펙트럼 점수 주입
    List<Cell> injectSpectralScores(List<Cell> cells, DocumentType type)

    // 문서 타입 자동 감지
    List<Cell> injectSpectralScoresAutoDetect(List<Cell> cells)

    // 구조 점수 계산
    private double calculateStructuralScore(Cell cell, double resonance)
}
```

**구조 점수 계산 공식**:
```
structuralScore = (baseImportance × 0.4) + (normalizedResonance × 0.6)
```

##### StructureValidator
```java
public class StructureValidator {
    // 구조 검증
    ValidationResult validate(List<Cell> cells)

    // 무결성 검증 (필수 필드 등)
    private void validateIntegrity(List<Cell> cells, ValidationResult result)

    // 구조적 일관성 검증
    private void validateStructuralConsistency(List<Cell> cells, ValidationResult result)
}
```

**검증 항목**:
- 필수 필드 존재 여부
- 위치 순서 일관성
- 점수 범위 (0.0 ~ 1.0)
- 공명 강도 유효성
- 계층 구조 무결성

**입력**: Cell 리스트 (임베딩 포함)
**출력**: Cell 리스트 (구조 점수 포함), 검증 결과

---

### Layer 5: Integration Layer

**목적**: LLM을 활용한 자연어 요약 및 결과 포맷팅

#### 주요 컴포넌트

##### LLMAdapter
```java
public class LLMAdapter {
    // 문서 요약 생성
    String generateSummary(List<Cell> cells)

    // 구조 설명 생성
    String explainStructure(List<Cell> cells, String validationResult)

    // 키워드 추출
    List<String> extractKeywords(List<Cell> cells)
}
```

**LLM 호출 최적화**:
- 구조 점수 > 0.6인 셀만 선택 (최대 20개)
- 프롬프트 엔지니어링으로 토큰 사용량 절감
- Fallback 메커니즘 (API 없이도 동작)

##### ResultFormatter
```java
public class ResultFormatter {
    // 전체 결과 포맷팅
    Map<String, Object> formatAnalysisResult(
        List<Cell> cells,
        String summary,
        ValidationResult validationResult
    )

    // 통계 정보 생성
    private Map<String, Object> formatStatistics(List<Cell> cells)
}
```

**출력 형식**:
- JSON 형태로 구조화
- 통계 정보, 셀 정보, 검증 결과, 요약 포함

**입력**: Cell 리스트 (구조 점수 포함)
**출력**: 최종 분석 결과 (JSON)

---

## 3. 핵심 알고리즘

### 3.1 Token Mixing

**수식**:
```
E'ᵢ = (Eᵢ₋₁ + Eᵢ + Eᵢ₊₁) / 3
```

**의사 코드**:
```java
for (int i = 0; i < n; i++) {
    for (int d = 0; d < dim; d++) {
        double sum = embeddings[i][d];
        int count = 1;

        if (i > 0) {
            sum += embeddings[i - 1][d];
            count++;
        }

        if (i < n - 1) {
            sum += embeddings[i + 1][d];
            count++;
        }

        mixed[i][d] = sum / count;
    }
}
```

### 3.2 FFT Resonance Analysis

**수식**:
```
R = IFFT(FFT(X) · P)
```
여기서:
- X: 셀 시퀀스 신호
- P: 문서 구조 필터
- R: 공명 강도

**의사 코드**:
```java
// 1. 셀 → 신호 변환
double[] signal = cellsToSignal(cells);

// 2. FFT 변환
FFTSpectrum spectrum = fft.transform(signal);

// 3. 필터 적용 (주파수 영역에서 곱셈)
for (int i = 0; i < size; i++) {
    spectrum.real[i] *= filter[i];
    spectrum.imag[i] *= filter[i];
}

// 4. IFFT로 역변환
double[] resonance = fft.inverseTransform(spectrum);
```

### 3.3 문서 타입 자동 감지

**알고리즘**:
```java
1. 타입별 카운트 수집 (헤더, 문단, 리스트 등)
2. 각 문서 타입별 점수 계산:
   - RESEARCH_PAPER: 문단 > 60%, 헤더 > 3개
   - REPORT: 균형잡힌 구조
   - CONTRACT: 리스트 > 30%
   - PRESENTATION: 짧은 셀, 헤더 많음
3. 최고 점수 타입 선택
```

---

## 4. 데이터 모델

### Cell 상세 구조

```java
public class Cell {
    // 기본 정보
    String id;                      // UUID
    BlockType type;                 // TITLE, SECTION_HEADER, PARAGRAPH, ...
    String content;                 // 원본 텍스트
    int position;                   // 문서 내 시작 위치
    int endPosition;                // 끝 위치
    Integer pageNumber;             // 페이지 번호 (선택)

    // 임베딩
    double[] embeddingVector;       // 차원: 128

    // 레이아웃 정보
    CellLayoutInfo layoutInfo;

    // 분석 결과
    double structuralScore;         // 0.0 ~ 1.0
    double resonanceIntensity;      // FFT 공명 강도
    double importance;              // 중요도

    // 계층 구조
    String parentCellId;            // 부모 셀 ID (선택)
}
```

### CellLayoutInfo

```java
public static class CellLayoutInfo {
    int lineCount;                  // 줄 수
    int wordCount;                  // 단어 수
    int charCount;                  // 문자 수
    int indentLevel;                // 들여쓰기 레벨
    double relativeFontSize;        // 상대적 폰트 크기
}
```

### FFTSpectrum

```java
public class FFTSpectrum {
    double[] complexSpectrum;       // [real0, imag0, real1, imag1, ...]
    int size;                       // 신호 크기
    List<Cell> originalCells;       // 원본 셀 참조
}
```

---

## 5. API 명세

### 5.1 POST /api/v1/analyze/upload

**요청**:
```
Content-Type: multipart/form-data

file: [PDF 파일]
documentType: RESEARCH_PAPER | REPORT | CONTRACT | PRESENTATION | GENERAL
```

**응답**:
```json
{
  "totalCells": 45,
  "summary": "문서 요약...",
  "statistics": {
    "cellTypeDistribution": { ... },
    "averageStructuralScore": "0.742",
    "averageResonanceIntensity": "1.523"
  },
  "validation": {
    "isValid": true,
    "errorCount": 0
  },
  "keySections": [ ... ],
  "cells": [ ... ]
}
```

### 5.2 POST /api/v1/analyze/quick

**설명**: LLM 없이 빠른 분석

**응답**:
```json
{
  "processingTimeMs": 1234,
  "summary": "간단한 요약",
  "cellCount": 45,
  "validation": { ... }
}
```

---

## 6. 성능 최적화 전략

### 6.1 시간 복잡도
- **FFT**: O(N log N)
- **Token Mixing**: O(N × D) (N: 토큰 수, D: 임베딩 차원)
- **전체 파이프라인**: O(N log N)

### 6.2 메모리 최적화
- 셀 단위 처리로 전체 문서를 메모리에 로드하지 않음
- 스트리밍 방식 지원

### 6.3 LLM 비용 절감
- 구조 점수 기반 필터링으로 중요한 셀만 LLM에 전달
- 최대 70% 토큰 절감

---

## 7. 확장 가능성

### 7.1 새로운 문서 타입 추가
```java
// 1. ResonanceFilter 생성
ResonanceFilter customFilter = createCustomFilter();

// 2. Registry에 등록
filterRegistry.registerFilter(customFilter);
```

### 7.2 새로운 임베딩 모델 통합
```java
// TokenMixer의 createSimpleEmbedding 메서드를 교체
// 예: Word2Vec, BERT, Sentence Transformers 등
```

### 7.3 다른 LLM 통합
```java
// LLMAdapter를 상속하여 새로운 어댑터 구현
public class CustomLLMAdapter extends LLMAdapter {
    // 사용자 정의 LLM 호출 로직
}
```

---

## 8. 테스트 전략

### 8.1 단위 테스트
- 각 계층의 컴포넌트별 독립 테스트
- Mock 객체를 사용한 의존성 격리

### 8.2 통합 테스트
- 전체 파이프라인 End-to-End 테스트
- 다양한 문서 타입 (논문, 보고서 등) 검증

### 8.3 성능 테스트
- 대용량 문서 (100+ 페이지) 처리 시간 측정
- 메모리 사용량 프로파일링

---

## 9. 참고 문헌

- Apache PDFBox Documentation
- JTransforms API Reference
- OpenAI API Documentation
- Fast Fourier Transform (FFT) 이론

---

**작성일**: 2025-11-10
**버전**: 1.0.0
