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

    // ★ Musical 엔티티와의 FK 연동
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "musical_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_rated_musical_musical")
    )
    private Musical musical;

    @Column(nullable = false)
    private Double rating;

    @Column(name="rated_at", nullable = false)
    private LocalDateTime ratedAt;
}