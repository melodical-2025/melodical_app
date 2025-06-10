package com.melodical.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicalRatingDto {
        @JsonProperty("musicalId")
        private String musicalId;

        private Double rating;
        private String title;
        private String artist;
    }
