package com.melodical.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicalRatingRequest {
        private Long userId;
        private String type;

        @JsonProperty("ratings")
        private List<MusicalRatingDto> musicalRatings;
    }

