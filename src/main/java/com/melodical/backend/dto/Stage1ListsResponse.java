package com.melodical.backend.dto;

import lombok.*;

import java.util.List;

/**
 * Stage1 단계별 리스트 응답 DTO
 * 음악 취향 기반, 뮤지컬 취향 기반, 인기차트 기반 리스트를 각각 반환
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stage1ListsResponse {
    
    /**
     * 사용자 ID
     */
    private Long userId;
    
    /**
     * 타임스탬프
     */
    private String timestamp;
    
    /**
     * 음악 취향 기반 추천 리스트
     */
    private List<Stage1Item> musicTasteList;
    
    /**
     * 뮤지컬 취향 기반 추천 리스트
     */
    private List<Stage1Item> musicalTasteList;
    
    /**
     * 인기차트 기반 추천 리스트
     */
    private List<Stage1Item> popularityList;
    
    /**
     * Stage1 개별 아이템
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Stage1Item {
        private Long musicalId;
        private String title;
        private String source;
        private Double stage1Score;
        private Double musicCfScore;
        private Double musicalCfScore;
        private Double popularityScore;
        private String genre;
        private String region;
        private Boolean isOnSale;
        private Boolean isNew;
    }
}
