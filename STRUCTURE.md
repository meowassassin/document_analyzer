# 프로젝트 구조

## 📁 디렉토리 구조

```
pdf_analyzer/
├── core/                       # ✅ 메인 모듈 (구현 완료)
│   └── src/main/java/com/pdfanalyzer/core/
│       ├── document/           # 문서 처리 (6 files)
│       │   ├── extractor/      - PDFExtractor
│       │   ├── analyzer/       - LayoutAnalyzer
│       │   └── model/          - DocumentMetadata, BlockType, LayoutInfo, LayoutStructure
│       │
│       ├── semantic/           # 의미 단위 처리 (3 files)
│       │   ├── model/          - SemanticCell
│       │   ├── embedding/      - TokenMixer
│       │   └── builder/        - CellBuilder
│       │
│       ├── frequency/          # 주파수 분석 (5 files)
│       │   ├── fft/            - FFTEngine, FFTSpectrum
│       │   └── filter/         - ResonanceFilter, DocumentType, ResonanceFilterRegistry
│       │
│       ├── analysis/           # 분석 및 검증 (2 files)
│       │   ├── scoring/        - SpectralScoreInjector
│       │   └── validation/     - StructureValidator
│       │
│       ├── integration/        # LLM 통합 (2 files)
│       │   ├── llm/            - LLMAdapter
│       │   └── formatter/      - ResultFormatter
│       │
│       ├── service/            # 비즈니스 로직 (1 file)
│       │   └── DocumentAnalysisService
│       │
│       ├── controller/         # REST API (2 files)
│       │   ├── AnalyzeController
│       │   └── PingController
│       │
│       ├── model/              # DTO (2 files)
│       │   ├── AnalysisResult
│       │   └── AnalyzeResponse
│       │
│       └── DocAnalyzerApplication.java
│
├── api-gateway/                # ❌ 향후 구현 예정
├── llm-service/                # ❌ 향후 구현 예정
├── data-storage/               # ❌ 향후 구현 예정
├── web-dashboard/              # ❌ 향후 구현 예정
│
├── pom.xml                     # 루트 POM (멀티모듈)
├── docker-compose.yml          # Docker 설정
├── README.md                   # 프로젝트 설명서
├── DESIGN.md                   # 상세 설계 문서
└── research_expanded.md        # 연구 보고서
```

## 🏗️ 아키텍처

### 5계층 구조

```
1️⃣ Document Processing    (document/)
   └─> PDF 추출, 레이아웃 분석

2️⃣ Semantic Cell          (semantic/)
   └─> 의미 단위 셀 생성, Token Mixing

3️⃣ Frequency Analysis     (frequency/)
   └─> FFT 변환, Resonance 필터

4️⃣ Analysis & Validation  (analysis/)
   └─> 스펙트럼 점수 주입, 구조 검증

5️⃣ Integration            (integration/)
   └─> LLM 요약, 결과 포맷팅
```

### 데이터 흐름

```
MultipartFile (PDF)
    ↓
[DocumentAnalysisService]
    ↓
1. PDFExtractor → List<String> pages
    ↓
2. CellBuilder → List<SemanticCell> cells
    ↓
3. FFTEngine + ResonanceFilter → 공명 강도
    ↓
4. SpectralScoreInjector → 구조 점수 주입
    ↓
5. StructureValidator → 검증
    ↓
6. LLMAdapter → 요약 생성
    ↓
7. ResultFormatter → Map<String, Object>
    ↓
[AnalyzeController] → JSON Response
```

## 📊 핵심 클래스

### 1. DocumentAnalysisService
```java
@Service
public class DocumentAnalysisService {
    public AnalysisResult analyze(MultipartFile file, DocumentType type)
    public AnalysisResult quickAnalyze(MultipartFile file)
}
```

### 2. SemanticCell
```java
@Data
public class SemanticCell {
    private String id;
    private BlockType type;
    private String content;
    private double[] embeddingVector;
    private double structuralScore;
    private double resonanceIntensity;
    // ...
}
```

### 3. FFTEngine
```java
@Component
public class FFTEngine {
    public FFTSpectrum transform(List<SemanticCell> cells)
    public double[] analyzeResonance(List<SemanticCell> cells, double[] filter)
}
```

## 🔧 기술 스택

| 항목 | 기술 | 버전 |
|------|------|------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.3.4 |
| PDF | Apache PDFBox | 3.0.1 |
| FFT | JTransforms | 3.1 |
| 수치연산 | ND4J | 1.0.0-M2.1 |
| LLM | OpenAI API | 0.18.2 |
| Build | Maven | 3.8+ |

## 🚀 실행 방법

### 1. 빌드
```bash
cd pdf_analyzer
mvn clean install
```

### 2. 실행
```bash
cd core
mvn spring-boot:run
```

### 3. API 테스트
```bash
# Health Check
curl http://localhost:8080/api/v1/analyze/health

# PDF 분석
curl -X POST http://localhost:8080/api/v1/analyze \
  -F "file=@test.pdf" \
  -F "documentType=RESEARCH_PAPER"
```

## 📝 API 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/v1/analyze/health` | Health check |
| GET | `/api/v1/analyze/document-types` | 문서 타입 목록 |
| POST | `/api/v1/analyze` | 전체 분석 (LLM 포함) |
| POST | `/api/v1/analyze/quick` | 빠른 분석 (LLM 제외) |
| GET | `/ping` | 기본 ping |

## 🎯 핵심 알고리즘

### Token Mixing
```
E'ᵢ = (Eᵢ₋₁ + Eᵢ + Eᵢ₊₁) / 3
```

### FFT Resonance
```
Y[k] = X[k] · P[k]
R = IFFT(Y)
```

### Structural Score
```
Score = (Importance × 0.4) + (Resonance × 0.6)
```

## 📈 성능 목표

- ⚡ 처리 속도: O(N log N)
- 🎯 구조 정확도: 97% 이상
- 💾 메모리 절감: 40~70%
- 💰 LLM 토큰 절감: 최대 70%

## 👥 팀

- **이수현** - FFT 엔진 개발
- **엄준경** - 시스템 설계
- **이태준** - 통합 및 검증

**기간**: 2025.11.10 ~ 2025.12.10

---

**Last Updated**: 2025-11-10
**Status**: ✅ Core 모듈 구현 완료, 빌드 성공
