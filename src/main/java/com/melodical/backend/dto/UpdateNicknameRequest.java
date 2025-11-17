package com.melodical.backend.dto;

import lombok.*;

/**
 * 닉네임 변경 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNicknameRequest {
    private String nickname;
}
