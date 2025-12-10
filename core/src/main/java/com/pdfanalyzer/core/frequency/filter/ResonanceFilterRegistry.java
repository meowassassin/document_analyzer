package com.pdfanalyzer.core.frequency.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 문서 타입별 필터를 등록하고 관리하는 레지스트리
 */
@Slf4j
@Component
public class ResonanceFilterRegistry {

    private final Map<DocumentType, ResonanceFilter> filters = new HashMap<>();

    public ResonanceFilterRegistry() {
        initializeFilters();
    }

    private void initializeFilters() {
        registerFilter(createResearchPaperFilter());
        registerFilter(createReportFilter());
        registerFilter(createContractFilter());
        registerFilter(createPresentationFilter());
        registerFilter(createGeneralFilter());

        log.info("필터 레지스트리 초기화 완료: {} 필터 등록", filters.size());
    }

    public void registerFilter(ResonanceFilter filter) {
        filters.put(filter.getDocumentType(), filter);
        log.debug("필터 등록: {} - {}", filter.getDocumentType(), filter.getName());
    }

    public ResonanceFilter getFilter(DocumentType type) {
        return filters.getOrDefault(type, createGeneralFilter());
    }

    private ResonanceFilter createResearchPaperFilter() {
        int size = 128;
        double[] coefficients = new double[size];

        for (int i = 0; i < size; i++) {
            double freq = (double) i / size;
            if (freq < 0.1) {
                coefficients[i] = 2.0;
            } else if (freq < 0.3) {
                coefficients[i] = 1.5;
            } else if (freq < 0.5) {
                coefficients[i] = 1.0;
            } else {
                coefficients[i] = 0.3;
            }
        }

        return ResonanceFilter.builder()
                .name("Research Paper Filter")
                .documentType(DocumentType.RESEARCH_PAPER)
                .coefficients(coefficients)
                .description("논문의 서론-본론-결론 구조를 강조하는 저주파 필터")
                .build();
    }

    private ResonanceFilter createReportFilter() {
        int size = 128;
        double[] coefficients = new double[size];

        for (int i = 0; i < size; i++) {
            double freq = (double) i / size;
            if (freq < 0.2) {
                coefficients[i] = 1.8;
            } else if (freq < 0.5) {
                coefficients[i] = 1.5;
            } else if (freq < 0.7) {
                coefficients[i] = 0.8;
            } else {
                coefficients[i] = 0.2;
            }
        }

        return ResonanceFilter.builder()
                .name("Report Filter")
                .documentType(DocumentType.REPORT)
                .coefficients(coefficients)
                .description("보고서의 요약-본문-결론 구조를 강조")
                .build();
    }

    private ResonanceFilter createContractFilter() {
        int size = 128;
        double[] coefficients = new double[size];

        for (int i = 0; i < size; i++) {
            double freq = (double) i / size;
            if (freq >= 0.2 && freq <= 0.4) {
                coefficients[i] = 2.0;
            } else if (freq < 0.6) {
                coefficients[i] = 1.2;
            } else {
                coefficients[i] = 0.4;
            }
        }

        return ResonanceFilter.builder()
                .name("Contract Filter")
                .documentType(DocumentType.CONTRACT)
                .coefficients(coefficients)
                .description("계약서의 반복적인 조항 구조를 강조")
                .build();
    }

    private ResonanceFilter createPresentationFilter() {
        int size = 128;
        double[] coefficients = new double[size];

        for (int i = 0; i < size; i++) {
            double freq = (double) i / size;
            if (freq < 0.3) {
                coefficients[i] = 1.0;
            } else if (freq < 0.7) {
                coefficients[i] = 1.8;
            } else {
                coefficients[i] = 0.5;
            }
        }

        return ResonanceFilter.builder()
                .name("Presentation Filter")
                .documentType(DocumentType.PRESENTATION)
                .coefficients(coefficients)
                .description("발표 자료의 슬라이드 단위 구조를 강조")
                .build();
    }

    private ResonanceFilter createGeneralFilter() {
        int size = 128;
        double[] coefficients = new double[size];

        for (int i = 0; i < size; i++) {
            coefficients[i] = 1.0;
        }

        return ResonanceFilter.builder()
                .name("General Filter")
                .documentType(DocumentType.GENERAL)
                .coefficients(coefficients)
                .description("모든 주파수 성분을 균등하게 통과")
                .build();
    }

    public Map<DocumentType, ResonanceFilter> getAllFilters() {
        return new HashMap<>(filters);
    }
}
