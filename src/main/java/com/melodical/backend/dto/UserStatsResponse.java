package com.melodical.backend.dto;

import lombok.*;

/**
 * 사용자 통계 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatsResponse {
    private Long userId;
    private String nickname;
    private String email;
    private String profileImageUrl;
    private int favoriteCount;      // 찜한 뮤지컬 개수
    private int ratedMusicalCount;  // 평가한 뮤지컬 개수
    private int ratedMusicCount;    // 평가한 음악 개수
}
