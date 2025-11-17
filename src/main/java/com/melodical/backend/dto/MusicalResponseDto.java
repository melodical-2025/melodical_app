package com.melodical.backend.dto;

import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor
public class MusicalResponseDto {
    private Long id;
    private String interparkId; // Interpark ID (8자리)
    private String cast;
    private String endDate;
    private String posterUrl;
    private String runtime;
    private String startDate;
    private String theater;
    private String title;
    
    // 평점 및 URL 정보
    private Double interparkRating;
    private Double yes24Rating;
    private String interparkUrl;
    private String yes24Url;
    private String period; // startDate ~ endDate
}
