package com.melodical.backend.dto;

import lombok.*;
import java.util.List;

/**
 * 추천 결과 DTO
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecommendationResponse {

    private Long musicalId;
    private String title;
    private String posterUrl;
    private String theater;
    private String startDate;
    private String endDate;
    private String genre;
    private List<String> tags;
    private String region;
    private Integer priceMin;
    private Integer priceMax;
    private Boolean isOnSale;
    private Boolean isNew;
    private Double popularityScore;

    // 추천 관련 점수들
    private Double finalScore; // 최종 점수
    private Double stage1Score; // Stage-1 점수
    private Double pctrScore; // pCTR 점수
    private Double contentCrossScore; // 콘텐츠 기반 점수
    private Double cfCrossScore; // 협업 필터링 점수

    // 추천 이유 (설명가능성)
    private List<String> reasons;
    private Integer position; // 추천 목록에서의 위치
    private String recommendationReason;  // 주요 추천 이유 (단일 문자열)
    private Double similarityPercentage;  // 유사도 퍼센트 (0-100)
    private Integer chartRanking;          // 인기차트 순위 (있는 경우)

    // Integrated 데이터 추가 필드 (평점 및 URL)
    private Double averageRating;    // 평균 평점
    private String interparkUrl;     // 인터파크 URL
    private String yes24Url;         // Yes24 URL

    // 메타데이터
    private String recommendationId; // 추천 세션 ID
    private Long timestamp; // 추천 시점
}
