package com.melodical.backend.dto;

import lombok.*;

/** SongDto + 사용자의 기존 평점(userRating) 함께 전달 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Getter
@Setter
public class SongResponseDto {
    private String id;
    private String title;
    private String artist;
    private String artworkUrl;
    private String genre;
    private Double userRating;  // null 이면 미평가
}