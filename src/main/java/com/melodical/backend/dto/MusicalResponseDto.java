package com.melodical.backend.dto;

import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor
public class MusicalResponseDto {
    private Long id;
    private String cast;
    private String endDate;
    private String posterUrl;
    private String runtime;
    private String startDate;
    private String theater;
    private String title;
}
