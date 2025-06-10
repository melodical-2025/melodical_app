package com.melodical.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    private String nickname;
    private String email;
    private String password;
}
