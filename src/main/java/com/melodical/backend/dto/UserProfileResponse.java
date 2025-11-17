package com.melodical.backend.dto;

import lombok.*;

/**
 * 사용자 프로필 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private Long userId;
    private String email;
    private String nickname;
    private String provider;  // local, google, kakao, naver
    private String providerId;
}
