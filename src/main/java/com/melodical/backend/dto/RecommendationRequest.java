package com.melodical.backend.dto;

import lombok.*;
import java.util.List;

/**
 * 추천 요청 DTO
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecommendationRequest {

    private Long userId;
    private String surface; // "home", "search", "detail" 등
    private Integer count; // 추천 개수 (기본값: 20)
    private String sessionId; // 세션 추적용
    private String region; // 지역 필터
    private List<String> excludeGenres; // 제외할 장르들
    private Boolean includeOnSaleOnly; // 판매중만 포함 여부

    // 기본값 설정
    public Integer getCount() {
        return count != null ? count : 20;
    }

    public String getSurface() {
        return surface != null ? surface : "home";
    }
}
