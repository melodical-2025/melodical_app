package com.melodical.backend.service;

import com.melodical.backend.config.AppleMusicProperties;
import com.melodical.backend.dto.SongDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AppleMusicService {

    private final WebClient webClient;
    private final String developerToken;
    private final String storefront = "kr";  // ① 하드코딩된 storefront

    public AppleMusicService(
            WebClient.Builder webClientBuilder,
            @Qualifier("developerToken") String developerToken  // ② developerToken 빈
    ) {
        this.webClient     = webClientBuilder
                .baseUrl("https://api.music.apple.com")
                .build();
        this.developerToken = developerToken;
    }

    /** 상위 50개 곡을 Apple Music Charts API 로부터 가져와 DTO 로 변환 */
    public List<SongDto> fetchTopSongs() {
        // 1) fetch raw JSON into a Map
        Map<String,Object> body = webClient.get()
                .uri("/v1/catalog/{storefront}/charts?types=songs&limit=50", storefront)
                .header("Authorization", "Bearer " + developerToken)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String,Object>>() {})
                .block();

        if (body == null) {
            return Collections.emptyList();
        }

        Map<String,Object> results = (Map<String,Object>) body.get("results");
        List<Map<String,Object>> songCharts = (List<Map<String,Object>>) results.get("songs");
        if (songCharts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String,Object>> data = (List<Map<String,Object>>) songCharts.get(0).get("data");

        return data.stream().map(item -> {
            Map<String,Object> attr = (Map<String,Object>) item.get("attributes");
            String urlTemplate = (String) ((Map<?,?>)attr.get("artwork")).get("url");
            String artworkUrl  = urlTemplate.replace("{w}x{h}", "200x200");
            String genre       = ((List<String>) attr.get("genreNames")).get(0);
            return new SongDto(
                    item.get("id").toString(),
                    attr.get("name").toString(),
                    attr.get("artistName").toString(),
                    artworkUrl,
                    genre
            );
        }).collect(Collectors.toList());
    }
}