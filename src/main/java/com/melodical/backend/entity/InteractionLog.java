package com.melodical.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 클릭/상호작용 로그
 * 사용자의 클릭, 평점, 찜하기 등의 행동을 추적
 */
@Entity
@Table(name = "interaction_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InteractionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "musical_id")
    private Musical musical;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exposure_log_id")
    private ExposureLog exposureLog; // 해당 노출에 대한 참조

    @Column(name = "interaction_type", nullable = false)
    private String interactionType; // "click", "rating", "wishlist", "purchase" 등

    @Column(name = "rating_value")
    private Double ratingValue; // 평점 (1.0 ~ 5.0, 0.5 단위)

    @Column(name = "interaction_at", nullable = false)
    private LocalDateTime interactionAt;

    @Column(name = "session_id")
    private String sessionId;

    @PrePersist
    protected void onCreate() {
        interactionAt = LocalDateTime.now();
    }
}
