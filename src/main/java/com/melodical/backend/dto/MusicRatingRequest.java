package com.melodical.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.*;

@Data @Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class MusicRatingRequest {
    private Long userId;
    private String type;

    @JsonProperty("ratings")
    private List<RatingDto> musicRatings;
}