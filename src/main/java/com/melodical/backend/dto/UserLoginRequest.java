// src/main/java/com/melodical/backend/dto/UserLoginRequest.java
package com.melodical.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserLoginRequest {
    private String email;
    private String password;
}
