package com.melodical.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 뮤지컬 팬층 프로필 (A_m)
 * 해당 뮤지컬을 높게 평가한 사용자들의 음악 취향 평균 벡터
 */
@Entity
@Table(name = "musical_fan_profile")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MusicalFanProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "musical_id")
    private Musical musical;

    @Column(name = "genre_name", nullable = false)
    private String genreName;

    @Column(name = "fan_preference_score", nullable = false)
    private Double fanPreferenceScore; // 팬들의 해당 장르 선호도 평균

    @Column(name = "fan_count", nullable = false)
    private Integer fanCount; // 해당 뮤지컬을 좋아하는 팬 수

    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore; // 신뢰도 (팬 수가 적으면 낮음)

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
