package com.melodical.backend.dto;

import lombok.*;
import java.util.Map;

/**
 * 사용자 음악 프로필 DTO
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserMusicProfileDto {

    private Long userId;
    private Map<String, Double> genrePreferences; // 장르명 -> 선호도 점수
    private Integer totalRatings; // 총 평점 개수
    private Double profileStrength; // 프로필 강도 (평점 개수 기반)

    // 최근 업데이트 정보
    private String lastUpdated;
    private Boolean isActive; // 최근 활동 여부

    // 벡터 정규화된 값들 (코사인 유사도 계산용)
    private Map<String, Double> normalizedPreferences;
}
