package com.pdfanalyzer.core.integration.llm;

import com.pdfanalyzer.core.semantic.model.SemanticCell;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LLMAdapter {

    private final RestTemplate restTemplate = new RestTemplate();
    private final com.pdfanalyzer.core.service.AnalysisCacheService cacheService;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-pro}")
    private String modelName;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    public LLMAdapter(com.pdfanalyzer.core.service.AnalysisCacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * 한 번의 API 호출로 요약과 키워드를 함께 생성 (캐싱 포함)
     */
    public SummaryAndKeywords generateSummaryAndKeywords(List<SemanticCell> cells) {
        // 문서 내용으로 해시 생성
        String documentContent = cells.stream()
            .map(SemanticCell::getContent)
            .collect(Collectors.joining("\n"));
        String documentHash = cacheService.generateDocumentHash(documentContent);

        // 캐시 확인
        if (documentHash != null) {
            com.pdfanalyzer.core.service.AnalysisCacheService.CachedAnalysis cached = cacheService.get(documentHash);
            if (cached != null) {
                log.info("캐시된 분석 결과 사용 (API 호출 생략)");
                return new SummaryAndKeywords(cached.getSummary(), cached.getKeywords(), cached.getKeywordLocations());
            }
        }

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Gemini API 키가 설정되지 않아 폴백 요약을 생성합니다.");
            return new SummaryAndKeywords(
                generateFallbackSummary(cells),
                generateFallbackKeywords(cells)
            );
        }

        try {
            List<SemanticCell> important = cells.stream()
                    .filter(c -> c.getStructuralScore() > 0.6)
                    .limit(20)
                    .collect(Collectors.toList());

            // 필터링된 셀이 없으면 모든 셀 사용
            if (important.isEmpty()) {
                log.info("구조 점수가 높은 셀이 없어 모든 셀 사용: {} 셀", cells.size());
                important = cells.stream().limit(20).collect(Collectors.toList());
            }

            String prompt = buildCombinedPrompt(important);
            log.debug("프롬프트 내용: {}", prompt);
            String response = callGeminiAPI(prompt);

            if (response.isEmpty()) {
                return new SummaryAndKeywords(
                    generateFallbackSummary(cells),
                    generateFallbackKeywords(cells)
                );
            }

            return parseCombinedResponse(response, cells, documentHash);

        } catch (Exception e) {
            log.error("LLM 호출 실패", e);
            return new SummaryAndKeywords(
                generateFallbackSummary(cells),
                generateFallbackKeywords(cells)
            );
        }
    }

    @Deprecated
    public String generateSummary(List<SemanticCell> cells) {
        return generateSummaryAndKeywords(cells).summary;
    }

    @Deprecated
    public List<String> extractKeywords(List<SemanticCell> cells) {
        return generateSummaryAndKeywords(cells).keywords;
    }

    private String buildCombinedPrompt(List<SemanticCell> cells) {
        StringBuilder sb = new StringBuilder();
        sb.append("다음 문서를 분석하여 JSON 형식으로 응답해주세요:\n\n");
        sb.append("요구사항:\n");
        sb.append("1. 문서의 핵심 내용을 3-5문장으로 요약 (summary)\n");
        sb.append("2. 핵심 키워드 5-10개 추출 (keywords 배열)\n\n");
        sb.append("응답 형식:\n");
        sb.append("{\n");
        sb.append("  \"summary\": \"문서 요약 내용\",\n");
        sb.append("  \"keywords\": [\"키워드1\", \"키워드2\", \"키워드3\"]\n");
        sb.append("}\n\n");
        sb.append("문서 내용:\n\n");

        for (SemanticCell cell : cells) {
            sb.append(cell.getContent().substring(0, Math.min(200, cell.getContent().length())))
              .append("\n\n");
        }
        return sb.toString();
    }

    private SummaryAndKeywords parseCombinedResponse(String response, List<SemanticCell> cells, String documentHash) {
        try {
            log.info("Gemini API 응답 내용: {}", response);

            // JSON 코드 블록 제거 (```json ... ```)
            response = response.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();

            // JSON 응답 파싱 시도
            int summaryStart = response.indexOf("\"summary\"");
            int keywordsStart = response.indexOf("\"keywords\"");

            if (summaryStart != -1 && keywordsStart != -1) {
                // summary 추출 - "summary": " 다음부터 시작하여 ", 까지 (따옴표로 감싼 값 추출)
                int summaryValueStart = response.indexOf("\"", response.indexOf(":", summaryStart) + 1) + 1;
                int summaryValueEnd = summaryValueStart;

                // 따옴표로 감싼 문자열 끝을 찾기 (이스케이프된 따옴표는 무시)
                while (summaryValueEnd < response.length()) {
                    summaryValueEnd = response.indexOf("\"", summaryValueEnd);
                    if (summaryValueEnd == -1 || response.charAt(summaryValueEnd - 1) != '\\') {
                        break;
                    }
                    summaryValueEnd++;
                }

                String summary = response.substring(summaryValueStart, summaryValueEnd).trim();

                // keywords 추출
                int keywordsArrayStart = response.indexOf("[", keywordsStart);
                int keywordsArrayEnd = response.indexOf("]", keywordsArrayStart);
                String keywordsStr = response.substring(keywordsArrayStart + 1, keywordsArrayEnd);
                List<String> keywords = Arrays.stream(keywordsStr.split(","))
                    .map(k -> k.replaceAll("\"", "").trim())
                    .filter(k -> !k.isEmpty())
                    .limit(10)
                    .collect(Collectors.toList());

                // 키워드 위치 정보 추출
                Map<String, List<KeywordLocation>> keywordLocations = findKeywordLocations(keywords, cells);

                // 캐시에 저장
                if (documentHash != null) {
                    cacheService.put(documentHash, summary, keywords, keywordLocations);
                }

                log.info("파싱 성공 - summary 길이: {}, keywords 수: {}", summary.length(), keywords.size());
                return new SummaryAndKeywords(summary, keywords, keywordLocations);
            } else {
                log.warn("JSON 필드를 찾을 수 없음 - summaryStart: {}, keywordsStart: {}", summaryStart, keywordsStart);
            }
        } catch (Exception e) {
            log.warn("응답 파싱 실패, 폴백 사용", e);
        }

        log.info("폴백 요약 사용");
        return new SummaryAndKeywords(
            generateFallbackSummary(cells),
            generateFallbackKeywords(cells)
        );
    }

    public static class SummaryAndKeywords {
        public final String summary;
        public final List<String> keywords;
        public final Map<String, List<KeywordLocation>> keywordLocations;

        public SummaryAndKeywords(String summary, List<String> keywords) {
            this.summary = summary;
            this.keywords = keywords;
            this.keywordLocations = new HashMap<>();
        }

        public SummaryAndKeywords(String summary, List<String> keywords, Map<String, List<KeywordLocation>> keywordLocations) {
            this.summary = summary;
            this.keywords = keywords;
            this.keywordLocations = keywordLocations;
        }
    }

    public static class KeywordLocation {
        public final String cellId;
        public final String content;
        public final int pageNumber;
        public final double relevanceScore;

        public KeywordLocation(String cellId, String content, int pageNumber, double relevanceScore) {
            this.cellId = cellId;
            this.content = content;
            this.pageNumber = pageNumber;
            this.relevanceScore = relevanceScore;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private String callGeminiAPI(String prompt) {
        try {
            String url = GEMINI_API_URL + modelName + ":generateContent?key=" + apiKey;

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, String> part = new HashMap<>();
            part.put("text", prompt);
            content.put("parts", Collections.singletonList(part));
            requestBody.put("contents", Collections.singletonList(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return extractTextFromResponse(response.getBody());
            }

            log.warn("Gemini API 응답이 비정상입니다: {}", response.getStatusCode());
            return "";

        } catch (Exception e) {
            log.error("Gemini API 호출 중 오류 발생", e);
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> responseBody) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                if (content != null) {
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }
        } catch (Exception e) {
            log.error("응답 파싱 중 오류", e);
        }
        return "";
    }

    private String generateFallbackSummary(List<SemanticCell> cells) {
        StringBuilder sb = new StringBuilder("[자동 요약]\n\n");
        cells.stream()
                .filter(SemanticCell::isHeader)
                .limit(5)
                .forEach(c -> sb.append("- ").append(c.getContent()).append("\n"));
        return sb.toString();
    }

    private List<String> generateFallbackKeywords(List<SemanticCell> cells) {
        return cells.stream()
                .filter(SemanticCell::isHeader)
                .map(SemanticCell::getContent)
                .limit(8)
                .collect(Collectors.toList());
    }

    /**
     * 키워드가 등장하는 셀 위치를 찾습니다
     */
    private Map<String, List<KeywordLocation>> findKeywordLocations(List<String> keywords, List<SemanticCell> cells) {
        Map<String, List<KeywordLocation>> locations = new HashMap<>();

        for (String keyword : keywords) {
            List<KeywordLocation> keywordLocs = new ArrayList<>();

            for (int i = 0; i < cells.size(); i++) {
                SemanticCell cell = cells.get(i);
                String content = cell.getContent().toLowerCase();
                String keywordLower = keyword.toLowerCase();

                // 키워드가 셀 내용에 포함되어 있는지 확인
                if (content.contains(keywordLower)) {
                    // 관련도 점수 계산 (키워드 출현 빈도 + 구조 점수)
                    int occurrences = countOccurrences(content, keywordLower);
                    double relevanceScore = (occurrences * 0.5) + (cell.getStructuralScore() * 0.5);

                    KeywordLocation location = new KeywordLocation(
                        cell.getId(),
                        cell.getContent().substring(0, Math.min(300, cell.getContent().length())), // 처음 300자
                        i + 1, // 페이지 번호 근사치 (실제로는 셀 순서)
                        relevanceScore
                    );
                    keywordLocs.add(location);
                }
            }

            // 관련도 점수로 정렬 (높은 순)
            keywordLocs.sort((a, b) -> Double.compare(b.relevanceScore, a.relevanceScore));

            // 상위 5개만 유지
            if (keywordLocs.size() > 5) {
                keywordLocs = keywordLocs.subList(0, 5);
            }

            locations.put(keyword, keywordLocs);
        }

        return locations;
    }

    private int countOccurrences(String text, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }
}
