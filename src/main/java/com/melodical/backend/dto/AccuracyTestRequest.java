package com.melodical.backend.dto;

import lombok.Data;

/**
 * 추천 알고리즘 정확도 테스트 요청 DTO
 */
@Data
public class AccuracyTestRequest {
    private Long userId;
    private Integer testDataSize = 20;
    private Double scoreThreshold = 0.7;
    private String testType = "precision"; // precision, recall, f1
}
