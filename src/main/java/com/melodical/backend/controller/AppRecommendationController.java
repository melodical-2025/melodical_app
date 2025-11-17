package com.melodical.backend.controller;

import com.melodical.backend.dto.RecommendationRequest;
import com.melodical.backend.dto.RecommendationResponse;
import com.melodical.backend.service.recommendation.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 플러터 앱용 추천 API 컨트롤러
 * 사용자 맞춤 추천 기능 제공
 */
@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
@Slf4j
public class AppRecommendationController {

    private final RecommendationService recommendationService;

    /**
     * 사용자 맞춤 추천
     */
    @PostMapping("/recommendations")
    public ResponseEntity<List<Map<String, Object>>> getRecommendations(
            @RequestBody Map<String, Object> request) {

        Long userId = Long.valueOf(request.get("userId").toString());
        String surface = request.getOrDefault("surface", "home").toString();
        Integer count = Integer.valueOf(request.getOrDefault("count", 20).toString());
        String region = request.getOrDefault("region", "서울").toString();

        log.info("Getting recommendations for user: {}, surface: {}, count: {}",
                userId, surface, count);

        RecommendationRequest recRequest = new RecommendationRequest();
        recRequest.setUserId(userId);
        recRequest.setSurface(surface);
        recRequest.setCount(count);
        recRequest.setRegion(region);

        List<RecommendationResponse> recommendations = recommendationService.recommend(recRequest);

        List<Map<String, Object>> response = recommendations.stream()
                .map(this::convertRecommendationToAppFormat)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * RecommendationResponse를 앱 형식으로 변환
     */
    private Map<String, Object> convertRecommendationToAppFormat(RecommendationResponse rec) {
        Map<String, Object> map = new HashMap<>();
        map.put("musicalId", rec.getMusicalId());
        map.put("title", rec.getTitle());

        // posterUrl 처리: //로 시작하면 https: 추가
        String posterUrl = rec.getPosterUrl();
        if (posterUrl != null && posterUrl.startsWith("//")) {
            posterUrl = "https:" + posterUrl;
        } else if (posterUrl == null || posterUrl.trim().isEmpty()) {
            try {
                posterUrl = "https://via.placeholder.com/300x400?text=" +
                        java.net.URLEncoder.encode(rec.getTitle(), "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                posterUrl = "https://via.placeholder.com/300x400?text=NoImage";
            }
        }
        map.put("posterUrl", posterUrl);

        map.put("theater", rec.getTheater());
        map.put("startDate", rec.getStartDate());
        map.put("endDate", rec.getEndDate());
        map.put("genre", rec.getGenre());
        map.put("region", rec.getRegion());
        map.put("priceMin", rec.getPriceMin());
        map.put("priceMax", rec.getPriceMax());
        map.put("isOnSale", rec.getIsOnSale());
        map.put("isNew", rec.getIsNew());
        map.put("score", rec.getFinalScore());
        map.put("popularityScore", rec.getPopularityScore());
        map.put("reasons", rec.getReasons());
        map.put("position", rec.getPosition());
        return map;
    }
}

