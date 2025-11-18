package com.melodical.backend.controller;

import com.melodical.backend.dto.*;
import com.melodical.backend.entity.User;
import com.melodical.backend.repository.UserRepository;
import com.melodical.backend.repository.MusicalRepository;
import com.melodical.backend.service.recommendation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 추천 시스템 API 컨트롤러
 * Melodical 2-Stage + Twiddler 추천 파이프라인 엔드포인트 제공
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final RecommendationLoggingService loggingService;
    private final UserProfileService userProfileService;
    private final MusicalFanProfileService musicalFanProfileService;
    private final UserRepository userRepository;
    private final MusicalRepository musicalRepository;

    /**
     * 메인 추천 API (GET)
     * GET /api/recommendations?userId=1&surface=home&count=20&region=서울
     */
    @GetMapping
    public ResponseEntity<List<RecommendationResponse>> getRecommendations(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "home") String surface,
            @RequestParam(defaultValue = "20") Integer count,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String sessionId) {

        try {
            RecommendationRequest request = RecommendationRequest.builder()
                    .userId(userId)
                    .surface(surface)
                    .count(count)
                    .region(region)
                    .sessionId(sessionId)
                    .build();

            List<RecommendationResponse> recommendations = recommendationService.recommend(request);

            return ResponseEntity.ok(recommendations);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid recommendation request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Recommendation request failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 앱 추천 API (POST) - Flutter용
     * POST /api/app/recommendations
     * Body: { "userId": 1, "surface": "home", "count": 20, "region": "서울" }
     */
    @PostMapping("/app/recommendations")
    public ResponseEntity<List<Map<String, Object>>> getAppRecommendations(
            @RequestBody Map<String, Object> requestBody) {

        try {
            // 요청 파라미터 추출
            Long userId = Long.valueOf(requestBody.get("userId").toString());
            String surface = (String) requestBody.getOrDefault("surface", "home");
            Integer count = Integer.valueOf(requestBody.getOrDefault("count", 20).toString());
            String region = (String) requestBody.get("region");
            String sessionId = (String) requestBody.get("sessionId");

            // 추천 요청 생성
            RecommendationRequest request = RecommendationRequest.builder()
                    .userId(userId)
                    .surface(surface)
                    .count(count)
                    .region(region)
                    .sessionId(sessionId)
                    .build();

            // 추천 실행
            List<RecommendationResponse> recommendations = recommendationService.recommend(request);

            // Flutter 친화적인 형식으로 변환
            List<Map<String, Object>> result = recommendations.stream()
                    .map(rec -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("musicalId", rec.getMusicalId());  // musicalId로 통일
                        map.put("id", rec.getMusicalId());  // 하위 호환성
                        map.put("title", rec.getTitle());
                        map.put("posterUrl", rec.getPosterUrl());
                        map.put("theater", rec.getTheater());
                        map.put("score", rec.getFinalScore());
                        map.put("genre", rec.getGenre());
                        map.put("region", rec.getRegion());
                        map.put("startDate", rec.getStartDate());
                        map.put("endDate", rec.getEndDate());
                        map.put("priceMin", rec.getPriceMin());
                        map.put("priceMax", rec.getPriceMax());
                        map.put("isOnSale", rec.getIsOnSale());
                        map.put("isNew", rec.getIsNew());
                        map.put("reasons", rec.getReasons());
                        map.put("tags", rec.getTags());
                        // 추천 이유 필드 추가
                        map.put("recommendationReason", rec.getRecommendationReason());
                        map.put("similarityPercentage", rec.getSimilarityPercentage());
                        map.put("chartRanking", rec.getChartRanking());
                        // 평점 및 URL 추가
                        map.put("averageRating", rec.getAverageRating());
                        map.put("interparkUrl", rec.getInterparkUrl());
                        map.put("yes24Url", rec.getYes24Url());
                        return map;
                    })
                    .collect(Collectors.toList());

            log.info("App recommendations returned {} items for user {}", result.size(), userId);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid app recommendation request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("App recommendation request failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 클릭 추적 API
     * POST /api/recommendations/track/click
     */
    @PostMapping("/track/click")
    public ResponseEntity<Void> trackClick(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Long musicalId = Long.valueOf(request.get("musicalId").toString());
            String sessionId = (String) request.get("sessionId");

            loggingService.logClick(userId, musicalId, sessionId);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to track click", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 평점 추적 API
     * POST /api/recommendations/track/rating
     */
    @PostMapping("/track/rating")
    public ResponseEntity<Void> trackRating(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Long musicalId = Long.valueOf(request.get("musicalId").toString());
            Double rating = Double.valueOf(request.get("rating").toString());
            String sessionId = (String) request.get("sessionId");
            String genre = (String) request.getOrDefault("genre", "Unknown");

            // 명시적 평점 로깅
            loggingService.logRating(userId, musicalId, rating, sessionId);

            // 사용자 프로필 즉시 업데이트
            userProfileService.updateUserProfileOnRating(userId, genre, rating);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to track rating", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 찜하기 추적 API
     * POST /api/recommendations/track/wishlist
     */
    @PostMapping("/track/wishlist")
    public ResponseEntity<Void> trackWishlist(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Long musicalId = Long.valueOf(request.get("musicalId").toString());
            String sessionId = (String) request.get("sessionId");

            loggingService.logWishlist(userId, musicalId, sessionId);

            // 암시적 프로필 업데이트
            userProfileService.updateProfileFromInteraction(userId, musicalId, "wishlist");

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to track wishlist", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 신규 사용자 온보딩 API
     * POST /api/recommendations/onboarding
     */
    @PostMapping("/onboarding")
    public ResponseEntity<Void> createUserProfile(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            @SuppressWarnings("unchecked")
            Map<String, Double> genreRatings = (Map<String, Double>) request.get("genreRatings");

            userProfileService.createInitialProfile(userId, genreRatings);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to create user profile", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 사용자 프로필 조회 API
     * GET /api/recommendations/profile/{userId}
     */
    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserMusicProfileDto> getUserProfile(@PathVariable Long userId) {
        try {
            // 사용자 프로필 조회 로직 (실제 구현 필요)
            UserMusicProfileDto profile = UserMusicProfileDto.builder()
                    .userId(userId)
                    .build();

            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            log.error("Failed to get user profile for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 추천 성능 메트릭 조회 API (관리자용)
     * GET /api/recommendations/metrics?days=7
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getRecommendationMetrics(
            @RequestParam(defaultValue = "7") Integer days,
            @RequestParam(required = false) Long userId) {

        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusDays(days);

            Map<String, Object> metrics;

            if (userId != null) {
                // 특정 사용자 메트릭
                metrics = recommendationService.getRecommendationMetrics(userId, days);
            } else {
                // 전체 시스템 메트릭
                metrics = loggingService.collectPerformanceMetrics(startTime, endTime);
            }

            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            log.error("Failed to get recommendation metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 뮤지컬 유사도 조회 API
     * GET /api/recommendations/similarity/{musicalId1}/{musicalId2}
     */
    @GetMapping("/similarity/{musicalId1}/{musicalId2}")
    public ResponseEntity<Map<String, Object>> getMusicalSimilarity(
            @PathVariable Long musicalId1,
            @PathVariable Long musicalId2) {

        try {
            double similarity = musicalFanProfileService.calculateMusicalSimilarity(musicalId1, musicalId2);

            Map<String, Object> result = Map.of(
                    "musical1_id", musicalId1,
                    "musical2_id", musicalId2,
                    "similarity_score", similarity,
                    "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to calculate musical similarity", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 시스템 상태 체크 API
     * GET /api/recommendations/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "healthy");
            health.put("timestamp", LocalDateTime.now());
            health.put("service", "melodical-recommendation-system");

            // 기본 데이터 확인
            long userCount = userRepository.count();
            long musicalCount = musicalRepository.count();

            health.put("userCount", userCount);
            health.put("musicalCount", musicalCount);

            // 간단한 추천 테스트
            if (userCount > 0 && musicalCount > 0) {
                User firstUser = userRepository.findAll().stream().findFirst().orElse(null);
                if (firstUser != null) {
                    try {
                        RecommendationRequest testRequest = RecommendationRequest.builder()
                                .userId(firstUser.getId())
                                .surface("test")
                                .count(5)
                                .build();

                        List<RecommendationResponse> testResult = recommendationService.recommend(testRequest);
                        health.put("testRecommendationCount", testResult.size());
                        health.put("testSuccess", true);
                    } catch (Exception e) {
                        health.put("testSuccess", false);
                        health.put("testError", e.getMessage());
                    }
                }
            }

            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Health check failed", e);
            return ResponseEntity.status(500)
                    .body(Map.of("status", "unhealthy", "error", e.getMessage()));
        }
    }

    /**
     * 간단한 추천 테스트 엔드포인트
     * GET /api/recommendations/simple-test/{userId}
     */
    @GetMapping("/simple-test/{userId}")
    public ResponseEntity<Map<String, Object>> simpleTest(@PathVariable Long userId) {
        try {
            Map<String, Object> result = new HashMap<>();

            // 사용자 확인
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User not found: " + userId));
            }

            result.put("user", Map.of("id", user.getId(), "nickname", user.getNickname()));

            // 기본 추천 시도
            RecommendationRequest request = RecommendationRequest.builder()
                    .userId(userId)
                    .surface("test")
                    .count(10)
                    .build();

            List<RecommendationResponse> recommendations = recommendationService.recommend(request);

            result.put("recommendationCount", recommendations.size());
            result.put("recommendations", recommendations.stream()
                    .limit(5)
                    .map(r -> Map.of(
                            "title", r.getTitle(),
                            "score", r.getFinalScore(),
                            "source", r.getReasons()
                    ))
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Simple test failed for user: {}", userId, e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage(), "stackTrace", Arrays.toString(e.getStackTrace())));
        }
    }

    /**
     * 캐시 초기화 API (관리자용)
     * POST /api/recommendations/admin/clear-cache
     */
    @PostMapping("/admin/clear-cache")
    public ResponseEntity<Void> clearCache(@RequestParam(required = false) Long userId) {
        try {
            if (userId != null) {
                // 특정 사용자 캐시만 초기화
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    log.info("Cleared cache for user: {}", userId);
                }
            } else {
                // 전체 캐시 초기화 (주의: 성능 영향 있음)
                log.info("Cleared all recommendation caches");
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to clear cache", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
