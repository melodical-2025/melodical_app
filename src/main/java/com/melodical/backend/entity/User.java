package com.melodical.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    private String password; // 소셜 로그인 사용자는 null 또는 "" 가능

    private String name;

    private String role; // ex) "USER", "ADMIN"

    // 소셜 로그인 사용자 정보 갱신 시 사용
    public User update(String name) {
        this.name = name;
        return this;
    }
}
