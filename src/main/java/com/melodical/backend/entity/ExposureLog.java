package com.melodical.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 추천 노출 로그
 * Stage-4에서 사용되는 노출 추적용 엔티티
 */
@Entity
@Table(name = "exposure_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExposureLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "musical_id")
    private Musical musical;

    @Column(name = "position", nullable = false)
    private Integer position; // 추천 목록에서의 위치 (1부터 시작)

    @Column(name = "surface", nullable = false)
    private String surface; // 노출 화면 (home, search, detail 등)

    @Column(name = "stage1_score")
    private Double stage1Score; // Stage-1 점수

    @Column(name = "pctr_score")
    private Double pctrScore; // Stage-2 pCTR 점수

    @Column(name = "final_score")
    private Double finalScore; // Twiddler 후 최종 점수

    @Column(name = "exposed_at", nullable = false)
    private LocalDateTime exposedAt;

    @Column(name = "session_id")
    private String sessionId; // 세션 추적용

    @PrePersist
    protected void onCreate() {
        exposedAt = LocalDateTime.now();
    }
}
