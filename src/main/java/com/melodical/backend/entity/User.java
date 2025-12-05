package com.melodical.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false ,unique = true)
    private String email;

    private String password; // 소셜 로그인 사용자는 null 또는 "" 가능

    private String name;

    @Column(name = "nickname")
    private String nickname; // 사용자 정의 닉네임
    
    @Column(name = "profile_image_url")
    private String profileImageUrl; // 프로필 이미지 URL

    private String role; // ex) "USER", "ADMIN"

    private String provider;
    
    private String providerId; // 소셜 로그인 제공자의 사용자 ID

    // 소셜 로그인 사용자 정보 갱신 시 사용
    public User update(String name) {
        this.name = name;
        return this;
    }

    public String getNickname() {
        // nickname이 설정되어 있으면 nickname 반환, 없으면 name 반환
        if (this.nickname != null && !this.nickname.isEmpty()) {
            return this.nickname;
        }
        return this.name != null && !this.name.isEmpty() ? this.name : "Unknown";
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}
