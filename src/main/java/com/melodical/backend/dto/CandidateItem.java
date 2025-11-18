package com.melodical.backend.dto;

import lombok.*;

/**
 * Stage-1에서 사용되는 후보 아이템 DTO
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateItem {

    private Long musicalId;
    private String title;

    // Stage-1 점수들
    private Double contentCrossScore; // 콘텐츠 기반 점수
    private Double cfCrossScore; // 협업 필터링 점수
    private Double sideScore; // 보조 점수 (지역, 판매중 등)
    private Double stage1Score; // 종합 점수
    
    // ✨ 새로운 평점 기반 CF 점수
    private Double musicCfScore;    // 음악 취향 기반 CF 점수 (30%)
    private Double musicalCfScore;  // 뮤지컬 취향 기반 CF 점수 (50%)

    // Stage-2 점수
    private Double pctrScore; // pCTR 예측 점수

    // Stage-3 점수
    private Double finalScore; // Twiddler 후처리 후 최종 점수

    // 메타데이터 (빠른 접근용)
    private String genre;
    private String region;
    private Boolean isOnSale;
    private Boolean isNew;
    private Double popularityScore;

    // 디버깅용
    private String source; // "content", "cf", "popularity" 등
    
    // 추천 이유 정보
    private String recommendationReason;  // 추천 이유 설명 (예: "음악 취향이 85% 일치하는 사용자가 좋아하는 작품입니다")
    private Double similarityPercentage;  // 유사도 퍼센트 (0-100)
    private Integer chartRanking;          // 인기차트 순위 (있는 경우)
}
