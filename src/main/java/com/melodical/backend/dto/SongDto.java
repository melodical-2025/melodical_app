package com.melodical.backend.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongDto {
    private String id;         // Apple Music song ID
    private String title;
    private String artist;
    private String artworkUrl;
    private String genre;
}