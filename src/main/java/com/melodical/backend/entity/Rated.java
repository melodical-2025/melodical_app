package com.melodical.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rated",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "content_id", "content_type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Rated {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** apple song id or internal musical id */
    @Column(name = "content_id", nullable = false)
    private String contentId;

    /** "music" or "musical" */
    @Column(name = "content_type", nullable = false)
    private String contentType;

    private String title;
    private String artist;
    private String genre;

    /** 0.5 단위, 0.0–5.0 */
    private Double rating;

    private LocalDateTime ratedAt;
}