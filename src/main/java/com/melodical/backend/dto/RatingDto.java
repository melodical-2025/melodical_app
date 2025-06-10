package com.melodical.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/** 각 곡별로 전달되는 평점 정보 */
@Data @Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class RatingDto {
    @JsonProperty("songId")
    private String contentId;

    private Double rating;
    private String title;
    private String artist;
}