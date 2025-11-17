package com.melodical.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 크롤링한 뮤지컬 랭킹 데이터
 * integrated_weekly_dataset, integrated_monthly_dataset 저장용
 */
@Entity
@Table(name = "crawled_musical_ranking",
       indexes = {
           @Index(name = "idx_interpark_id", columnList = "interpark_id"),
           @Index(name = "idx_ranking_type_version", columnList = "ranking_type, data_version"),
           @Index(name = "idx_combined_rank", columnList = "combined_rank")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawledMusicalRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title; // 뮤지컬 제목

    @Column(name = "interpark_id")
    private String interparkId; // Interpark URL의 마지막 8자리 숫자

    @Column(name = "normalized_title")
    private String normalizedTitle; // 정규화된 제목

    @Column(name = "interpark_title")
    private String interparkTitle; // 인터파크 제목

    @Column(name = "yes24_title")
    private String yes24Title; // Yes24 제목

    @Column(name = "ranking_type", nullable = false)
    private String rankingType; // WEEKLY, MONTHLY

    @Column(name = "combined_rank")
    private Integer combinedRank; // 통합 순위

    @Column(name = "interpark_rank")
    private Integer interparkRank;

    @Column(name = "yes24_rank")
    private Integer yes24Rank;

    @Column(name = "average_rating")
    private Double averageRating; // 평균 평점

    @Column(name = "interpark_rating")
    private Double interparkRating;

    @Column(name = "yes24_rating")
    private Double yes24Rating;

    @Column(name = "total_reviews")
    private Integer totalReviews; // 총 리뷰 수

    @Column(name = "interpark_reviews")
    private Integer interparkReviews;

    @Column(name = "yes24_reviews")
    private Integer yes24Reviews;

    @Column(name = "theater_name")
    private String theaterName; // 공연장

    @Column(name = "performance_period")
    private String performancePeriod; // 공연 기간

    @Column(length = 500)
    private String genre; // 장르

    @Column(length = 2000)
    private String description; // 설명

    @Column(name = "poster_url", length = 1000)
    private String posterUrl; // 포스터 URL

    @Column(name = "interpark_url", length = 1000)
    private String interparkUrl;

    @Column(name = "yes24_url", length = 1000)
    private String yes24Url;

    @Column(name = "is_available")
    private Boolean isAvailable; // 예매 가능 여부

    @Column(name = "crawled_at", nullable = false)
    private LocalDateTime crawledAt; // 크롤링 시간

    @Column(name = "data_version", nullable = false)
    private String dataVersion; // 데이터 버전 (타임스탬프 기반)

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

