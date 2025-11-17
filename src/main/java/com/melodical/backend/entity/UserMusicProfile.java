package com.melodical.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 사용자 음악 취향 벡터 (U_music)
 * Apple Music 장르/곡 평점을 기반으로 한 사용자의 음악 선호도 프로필
 */
@Entity
@Table(name = "user_music_profile")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserMusicProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "genre_name", nullable = false)
    private String genreName; // 장르명 (예: "Rock", "Pop", "Classical" 등)

    @Column(name = "preference_score", nullable = false)
    private Double preferenceScore; // 0.0 ~ 5.0 (시간감쇠 적용된 가중평균)

    @Column(name = "rating_count", nullable = false)
    private Integer ratingCount; // 해당 장르 평점 개수

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
