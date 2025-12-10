# PDF Analyzer - Cell-Frequency Hybrid Parser

FFT 기반 문서 구조 분석 및 의미론적 처리를 수행하는 고급 PDF 분석 시스템입니다.

## 시스템 아키텍처

### 마이크로서비스 구조

```
┌─────────────────┐
│  Web Dashboard  │ (React + Vite)
│    Port: 80     │
└────────┬────────┘
         │
┌────────▼────────┐
│  API Gateway    │ (Spring Cloud Gateway)
│   Port: 9090    │
└────────┬────────┘
         │
    ┌────┴────┬────────────┬─────────────┐
    │         │            │             │
┌───▼──┐  ┌──▼───┐   ┌────▼─────┐  ┌───▼────────┐
│ Core │  │ LLM  │   │  Data    │  │ PostgreSQL │
│ 8080 │  │ 8081 │   │ Storage  │  │    5432    │
└──────┘  └──────┘   │  8083    │  └────────────┘
                     └──────────┘
```

### 모듈 설명

- **core**: PDF 추출, FFT 분석, 셀 생성, 구조 검증
- **api-gateway**: API 라우팅 및 통합
- **llm-service**: Google Gemini 기반 요약 및 키워드 추출
- **data-storage**: 분석 결과 저장 및 조회 (JPA + PostgreSQL)
- **web-dashboard**: React 기반 사용자 인터페이스

## 주요 기능

### 1. Cell-Frequency Hybrid Parser
- **O(N log N)** 복잡도의 FFT 기반 문서 분석
- 의미론적 셀 생성 및 토큰 믹싱
- 공명 주파수 분석 (Resonance Analysis)

### 2. 문서 타입별 분석
- RESEARCH_PAPER: 논문
- REPORT: 보고서
- CONTRACT: 계약서
- PRESENTATION: 발표자료
- MANUAL: 매뉴얼
- GENERAL: 일반 문서

### 3. AI 기반 처리
- Google Gemini를 활용한 자동 요약
- 핵심 키워드 추출
- 문서 구조 검증

## 빠른 시작

### 사전 요구사항

- Docker & Docker Compose
- Java 21 (로컬 개발 시)
- Node.js 18+ (프론트엔드 개발 시)
- Maven 3.8+ (로컬 빌드 시)

### Docker Compose로 실행

1. 환경 변수 설정
```bash
cp .env.example .env
# .env 파일에서 다음 설정:
# - GEMINI_API_KEY: Google Gemini API 키
# - GOOGLE_CLOUD_PROJECT: Google Cloud 프로젝트 ID
# - GOOGLE_APPLICATION_CREDENTIALS: 서비스 계정 JSON 파일 경로
```

2. 빌드 및 실행
```bash
# Maven 빌드
mvn clean package -DskipTests

# Docker Compose 실행
docker-compose up -d
```

3. 서비스 확인
- Web Dashboard: http://localhost
- API Gateway: http://localhost:9090
- Core Service: http://localhost:8080
- LLM Service: http://localhost:8081
- Data Storage: http://localhost:8083

### 로컬 개발 모드

#### 백엔드 서비스

```bash
# Core Service
cd core
mvn spring-boot:run

# LLM Service
cd llm-service
mvn spring-boot:run

# Data Storage
cd data-storage
mvn spring-boot:run

# API Gateway
cd api-gateway
mvn spring-boot:run
```

#### 프론트엔드

```bash
cd web-dashboard
npm install
npm run dev
```

프론트엔드는 http://localhost:3000에서 실행됩니다.

## API 엔드포인트

### 분석 API (Core Service)

#### 문서 분석
```bash
POST /api/v1/analyze
Content-Type: multipart/form-data

Parameters:
- file: PDF 파일
- documentType: 문서 유형 (옵션)

Response:
{
  "analysisId": "123",
  "fileName": "sample.pdf",
  "documentType": "RESEARCH_PAPER",
  "summary": "...",
  "keywords": ["키워드1", "키워드2"],
  "avgStructuralScore": 0.85,
  "totalCells": 120,
  ...
}
```

#### 빠른 분석 (LLM 제외)
```bash
POST /api/v1/analyze/quick
Content-Type: multipart/form-data

Parameters:
- file: PDF 파일
```

#### Health Check
```bash
GET /api/v1/analyze/health
```

### 저장소 API (Data Storage)

#### 결과 조회
```bash
GET /api/v1/storage/results/{id}
GET /api/v1/storage/results/recent
GET /api/v1/storage/results/search?fileName={name}
```

#### 결과 저장
```bash
POST /api/v1/storage/results
Content-Type: application/json
```

### LLM API

#### 요약 생성
```bash
POST /api/v1/llm/summarize
Content-Type: application/json

{
  "text": "요약할 텍스트..."
}
```

#### 키워드 추출
```bash
POST /api/v1/llm/keywords
Content-Type: application/json

{
  "text": "분석할 텍스트..."
}
```

## 기술 스택

### 백엔드
- **Java 21**
- **Spring Boot 3.3.4**
- **Spring Cloud Gateway**
- **Spring Data JPA**
- **Apache PDFBox 3.0.1** - PDF 처리
- **JTransforms 3.1** - FFT 연산
- **ND4J 1.0.0-M2.1** - 수치 계산
- **Google Gemini API** - AI 기반 요약 및 키워드 추출
- **Google Cloud Vertex AI** - 엔터프라이즈급 AI 플랫폼
- **PostgreSQL 15** - 데이터 저장
- **H2** - 개발/테스트용 DB

### 프론트엔드
- **React 18**
- **Vite 5**
- **React Router 6**
- **Axios** - HTTP 클라이언트
- **Recharts** - 데이터 시각화
- **Lucide React** - 아이콘

### 인프라
- **Docker**
- **Docker Compose**
- **Nginx** - 프론트엔드 서빙

## 알고리즘 상세

### 1. Token Mixing
```
E'ᵢ = (Eᵢ₋₁ + Eᵢ + Eᵢ₊₁) / 3
```
주변 임베딩 벡터와의 평균을 통한 문맥 정보 보강

### 2. FFT 변환
```
Y[k] = Σ(x[n] · e^(-2πikn/N))
```
O(N log N) 복잡도로 주파수 도메인 변환

### 3. 공명 분석
```
Y[k] = X[k] · P[k]
R = IFFT(Y)
```
문서 타입별 필터 적용 후 역변환

### 4. 구조 점수 계산
```
StructuralScore = ResonanceIntensity × TypeWeight × PositionFactor
```

## 개발 가이드

### 프로젝트 구조

```
pdf_analyzer/
├── core/                      # 핵심 분석 엔진
│   └── src/main/java/com/pdfanalyzer/core/
│       ├── document/         # PDF 추출, 레이아웃 분석
│       ├── semantic/         # 셀 생성, 토큰 믹싱
│       ├── frequency/        # FFT, 공명 필터
│       ├── analysis/         # 점수 계산, 검증
│       └── integration/      # LLM 통합, 결과 포매팅
├── api-gateway/              # API 게이트웨이
├── llm-service/              # LLM 전담 서비스
├── data-storage/             # 데이터 저장 서비스
├── web-dashboard/            # React 웹 인터페이스
└── docker-compose.yml        # 전체 시스템 구성
```

### 빌드

```bash
# 전체 빌드
mvn clean package

# 모듈별 빌드
mvn clean package -pl core
mvn clean package -pl llm-service
mvn clean package -pl data-storage
mvn clean package -pl api-gateway

# 테스트 제외 빌드
mvn clean package -DskipTests
```

### 테스트

```bash
# 전체 테스트
mvn test

# 특정 모듈 테스트
mvn test -pl core
```

## 문제 해결

### Gemini API 키 오류
```
API 키가 설정되지 않아 요약을 생성할 수 없습니다.
```
- `.env` 파일에 `GEMINI_API_KEY` 설정 확인
- Google Cloud 프로젝트 설정 확인
- 서비스 계정 JSON 파일 경로 확인 (`GOOGLE_APPLICATION_CREDENTIALS`)
- 환경 변수가 컨테이너에 전달되는지 확인

### Google Cloud 인증 오류
- 서비스 계정에 필요한 권한이 있는지 확인:
  - Vertex AI User
  - AI Platform Prediction Service Agent
- `gcloud auth application-default login` 실행 (로컬 개발 시)

### 데이터베이스 연결 오류
- PostgreSQL 컨테이너가 실행 중인지 확인
- Health check 통과 여부 확인: `docker-compose ps`

### 포트 충돌
- 기본 포트(80, 5432, 8080-8083, 9090)가 사용 중인 경우 docker-compose.yml에서 포트 변경

## 라이센스

이 프로젝트는 연구 및 교육 목적으로 개발되었습니다.

## 참고 문헌

- research_expanded.md - Cell-Frequency Hybrid Parser 상세 설명
