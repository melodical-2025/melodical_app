package com.melodical.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rated_musical",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","musical_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RatedMusical {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ★ User 엔티티와의 FK 연동
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_rated_musical_user")
    )
    private User user;

    // ★ Musical ID를 Interpark ID(8자리)로 저장
    // 주의: 외래키 아님! Musical 테이블의 interpark_id 값을 저장하는 일반 문자열 컬럼
    @Column(name = "musical_id", nullable = false, length = 50)
    private String musicalId; // Interpark ID 문자열 저장 (FK가 아님)

    @Column(nullable = false)
    private Double rating;

    @Column(name="rated_at", nullable = false)
    private LocalDateTime ratedAt;
    
    @PrePersist
    protected void onCreate() {
        if (ratedAt == null) {
            ratedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        ratedAt = LocalDateTime.now();
    }
}
