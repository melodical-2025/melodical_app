package com.melodical.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "musical")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Musical {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "interpark_id", unique = true)
    private String interparkId; // Interpark URL의 마지막 8자리 숫자

    @Column(name = "interpark_url")
    private String interparkUrl; // Interpark 티켓 페이지 URL

    @Column(name = "yes24_url")
    private String yes24Url; // Yes24 티켓 페이지 URL

    private String cast;
    private String endDate;
    private String posterUrl;
    private String runtime;
    private String startDate;
    private String theater;
    private String title;

    // 추천 시스템을 위한 추가 메타데이터
    @Column(name = "genre")
    private String genre; // 뮤지컬 장르 (예: "로맨스", "코미디", "드라마" 등)

    @Column(name = "tags")
    private String tags; // 태그들 (콤마로 구분, 예: "가족뮤지컬,클래식,브로드웨이")

    @Column(name = "series_id")
    private String seriesId; // 시리즈 식별자 (같은 작품의 다른 시즌/버전)

    @Column(name = "producer")
    private String producer; // 제작사

    @Column(name = "age_rating")
    private String ageRating; // 연령 등급 (전체관람가, 12세이상 등)

    @Column(name = "price_min")
    private Integer priceMin; // 최저가격

    @Column(name = "price_max")
    private Integer priceMax; // 최고가격

    @Column(name = "region")
    private String region; // 공연 지역 (서울, 부산 등)

    @Column(name = "is_on_sale")
    private Boolean isOnSale; // 현재 판매중 여부

    @Column(name = "is_new")
    private Boolean isNew; // 신작 여부

    @Column(name = "popularity_score")
    private Double popularityScore; // 인기 점수 (조회수, 예매율 기반)

    @Column(name = "view_count")
    private Long viewCount; // 조회 수

    @Column(name = "booking_rate")
    private Double bookingRate; // 예매율

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (viewCount == null) viewCount = 0L;
        if (popularityScore == null) popularityScore = 0.0;
        if (bookingRate == null) bookingRate = 0.0;
        if (isOnSale == null) isOnSale = false;
        if (isNew == null) isNew = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
