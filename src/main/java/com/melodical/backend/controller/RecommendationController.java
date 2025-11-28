package com.melodical.backend.controller;

import com.melodical.backend.dto.*;
import com.melodical.backend.entity.User;
import com.melodical.backend.repository.UserRepository;
import com.melodical.backend.repository.MusicalRepository;
import com.melodical.backend.service.recommendation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final Stage1CandidateService stage1CandidateService;
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
    
    /**
     * 사용자별 추천 리스트 및 상세 유사도 조회 API (디버깅/분석용)
     * GET /api/recommendations/debug/{userId}
     * 
     * 반환 정보:
     * - 추천된 뮤지컬 목록 (17개)
     * - 각 추천의 상세 점수 (음악 CF, 뮤지컬 CF, 인기도)
     * - 추천 소스 (통합/음악만/인기만)
     * - 유사도 퍼센트
     * - 제외된 항목 수 (평가한 작품, 관심 작품)
     */
    @GetMapping("/debug/{userId}")
    public ResponseEntity<Map<String, Object>> getDebugRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "home") String surface,
            @RequestParam(defaultValue = "17") Integer count) {
        
        try {
            // 사용자 확인
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User not found: " + userId));
            }
            
            Map<String, Object> result = new HashMap<>();
            
            // 사용자 기본 정보
            result.put("userId", userId);
            result.put("nickname", user.getNickname());
            result.put("timestamp", LocalDateTime.now().toString());
            
            // 추천 생성
            RecommendationRequest request = RecommendationRequest.builder()
                    .userId(userId)
                    .surface(surface)
                    .count(count)
                    .build();
            
            List<RecommendationResponse> recommendations = recommendationService.recommend(request);
            
            // 추천 결과를 상세 정보와 함께 구성
            List<Map<String, Object>> detailedRecommendations = recommendations.stream()
                    .map(rec -> {
                        Map<String, Object> detail = new LinkedHashMap<>();
                        detail.put("position", rec.getPosition());
                        detail.put("musicalId", rec.getMusicalId());
                        detail.put("title", rec.getTitle());
                        detail.put("genre", rec.getGenre());
                        
                        // 점수 상세 정보
                        Map<String, Object> scores = new LinkedHashMap<>();
                        scores.put("finalScore", String.format("%.4f", rec.getFinalScore()));
                        scores.put("stage1Score", String.format("%.4f", rec.getStage1Score()));
                        scores.put("pctrScore", String.format("%.4f", rec.getPctrScore()));
                        scores.put("musicCfScore", String.format("%.4f", rec.getContentCrossScore())); // musicCfScore가 contentCrossScore에 매핑됨
                        scores.put("musicalCfScore", String.format("%.4f", rec.getCfCrossScore())); // musicalCfScore가 cfCrossScore에 매핑됨
                        detail.put("scores", scores);
                        
                        // 추천 이유 및 유사도
                        detail.put("recommendationReason", rec.getRecommendationReason());
                        detail.put("similarityPercentage", rec.getSimilarityPercentage() != null ? 
                                String.format("%.1f%%", rec.getSimilarityPercentage()) : "N/A");
                        detail.put("chartRanking", rec.getChartRanking());
                        
                        // 추천 소스 분석
                        String source = "unknown";
                        if (rec.getRecommendationReason() != null) {
                            if (rec.getRecommendationReason().contains("음악 취향") && 
                                rec.getRecommendationReason().contains("뮤지컬 취향")) {
                                source = "combined_algorithm";
                            } else if (rec.getRecommendationReason().contains("음악 취향")) {
                                source = "music_taste_only";
                            } else if (rec.getRecommendationReason().contains("인기차트")) {
                                source = "popularity_only";
                            }
                        }
                        detail.put("source", source);
                        
                        // 기타 정보
                        detail.put("posterUrl", rec.getPosterUrl());
                        detail.put("theater", rec.getTheater());
                        detail.put("averageRating", rec.getAverageRating());
                        
                        return detail;
                    })
                    .collect(Collectors.toList());
            
            result.put("recommendations", detailedRecommendations);
            result.put("totalCount", detailedRecommendations.size());
            
            // 추천 소스별 개수 집계
            Map<String, Long> sourceDistribution = detailedRecommendations.stream()
                    .collect(Collectors.groupingBy(
                            rec -> (String) rec.get("source"),
                            Collectors.counting()
                    ));
            result.put("sourceDistribution", sourceDistribution);
            
            // 평균 유사도 계산
            OptionalDouble avgSimilarity = detailedRecommendations.stream()
                    .map(rec -> (String) rec.get("similarityPercentage"))
                    .filter(sim -> !sim.equals("N/A"))
                    .mapToDouble(sim -> Double.parseDouble(sim.replace("%", "")))
                    .average();
            
            if (avgSimilarity.isPresent()) {
                result.put("averageSimilarityPercentage", String.format("%.1f%%", avgSimilarity.getAsDouble()));
            } else {
                result.put("averageSimilarityPercentage", "N/A");
            }
            
            log.info("Debug recommendations generated for user {}: {} items", userId, detailedRecommendations.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Failed to generate debug recommendations for user: {}", userId, e);
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "error", e.getMessage(),
                            "userId", userId,
                            "timestamp", LocalDateTime.now().toString()
                    ));
        }
    }
    
    /**
     * 전체 사용자 추천 분석 API (관리자/분석용)
     * GET /api/recommendations/analyze/all-users
     * 
     * 모든 사용자의 추천 결과를 요약하여 반환
     */
    @GetMapping("/analyze/all-users")
    public ResponseEntity<Map<String, Object>> analyzeAllUsers(
            @RequestParam(defaultValue = "10") Integer limit) {
        
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("timestamp", LocalDateTime.now().toString());
            
            List<User> users = userRepository.findAll().stream()
                    .limit(limit)
                    .collect(Collectors.toList());
            
            List<Map<String, Object>> userAnalyses = new ArrayList<>();
            
            for (User user : users) {
                try {
                    Map<String, Object> userAnalysis = new LinkedHashMap<>();
                    userAnalysis.put("userId", user.getId());
                    userAnalysis.put("nickname", user.getNickname());
                    
                    // 추천 생성
                    RecommendationRequest request = RecommendationRequest.builder()
                            .userId(user.getId())
                            .surface("home")
                            .count(17)
                            .build();
                    
                    List<RecommendationResponse> recommendations = recommendationService.recommend(request);
                    
                    // 통계 정보
                    userAnalysis.put("recommendationCount", recommendations.size());
                    
                    // 추천 소스별 개수
                    Map<String, Long> sourceCount = new HashMap<>();
                    sourceCount.put("combined", 0L);
                    sourceCount.put("musicOnly", 0L);
                    sourceCount.put("popularityOnly", 0L);
                    
                    for (RecommendationResponse rec : recommendations) {
                        if (rec.getRecommendationReason() != null) {
                            if (rec.getRecommendationReason().contains("음악 취향") && 
                                rec.getRecommendationReason().contains("뮤지컬 취향")) {
                                sourceCount.put("combined", sourceCount.get("combined") + 1);
                            } else if (rec.getRecommendationReason().contains("음악 취향")) {
                                sourceCount.put("musicOnly", sourceCount.get("musicOnly") + 1);
                            } else if (rec.getRecommendationReason().contains("인기차트")) {
                                sourceCount.put("popularityOnly", sourceCount.get("popularityOnly") + 1);
                            }
                        }
                    }
                    userAnalysis.put("sourceDistribution", sourceCount);
                    
                    // 평균 유사도
                    OptionalDouble avgSim = recommendations.stream()
                            .filter(rec -> rec.getSimilarityPercentage() != null)
                            .mapToDouble(RecommendationResponse::getSimilarityPercentage)
                            .average();
                    
                    if (avgSim.isPresent()) {
                        userAnalysis.put("averageSimilarity", String.format("%.1f%%", avgSim.getAsDouble()));
                    } else {
                        userAnalysis.put("averageSimilarity", "N/A");
                    }
                    
                    // Top 3 추천 제목
                    List<String> top3Titles = recommendations.stream()
                            .limit(3)
                            .map(RecommendationResponse::getTitle)
                            .collect(Collectors.toList());
                    userAnalysis.put("top3Recommendations", top3Titles);
                    
                    userAnalyses.add(userAnalysis);
                    
                } catch (Exception e) {
                    log.warn("Failed to analyze user {}: {}", user.getId(), e.getMessage());
                }
            }
            
            result.put("analyzedUserCount", userAnalyses.size());
            result.put("users", userAnalyses);
            
            // 전체 통계
            Map<String, Object> overallStats = new HashMap<>();
            double totalRecommendations = userAnalyses.stream()
                    .mapToInt(u -> (Integer) u.get("recommendationCount"))
                    .sum();
            overallStats.put("totalRecommendations", totalRecommendations);
            overallStats.put("avgRecommendationsPerUser", totalRecommendations / userAnalyses.size());
            
            result.put("overallStatistics", overallStats);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Failed to analyze all users", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 사용자별 추천 데이터 CSV 다운로드 API
     * GET /api/recommendations/export/csv/{userId}
     * 
     * CSV 파일로 추천 데이터 다운로드
     */
    @GetMapping("/export/csv/{userId}")
    public ResponseEntity<String> exportRecommendationsToCsv(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "home") String surface,
            @RequestParam(defaultValue = "17") Integer count) {
        
        try {
            // 사용자 확인
            if (userId == null) {
                return ResponseEntity.badRequest().body("User ID is required");
            }
            
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found: " + userId);
            }
            
            // 추천 생성
            RecommendationRequest request = RecommendationRequest.builder()
                    .userId(userId)
                    .surface(surface)
                    .count(count)
                    .build();
            
            List<RecommendationResponse> recommendations = recommendationService.recommend(request);
            
            // CSV 생성
            StringBuilder csv = new StringBuilder();
            
            // UTF-8 BOM 추가 (Excel에서 한글 깨짐 방지)
            csv.append("\uFEFF");
            
            // 헤더
            csv.append("순위,뮤지컬ID,제목,장르,극장,최종점수,Stage1점수,pCTR점수,음악CF점수,뮤지컬CF점수,");
            csv.append("추천이유,유사도,차트순위,추천소스,평균평점,포스터URL,시작일,종료일\n");
            
            // 데이터
            for (RecommendationResponse rec : recommendations) {
                // 추천 소스 판별
                String source = "unknown";
                if (rec.getRecommendationReason() != null) {
                    if (rec.getRecommendationReason().contains("음악 취향") && 
                        rec.getRecommendationReason().contains("뮤지컬 취향")) {
                        source = "통합알고리즘";
                    } else if (rec.getRecommendationReason().contains("음악 취향")) {
                        source = "음악취향전용";
                    } else if (rec.getRecommendationReason().contains("인기차트")) {
                        source = "인기차트전용";
                    }
                }
                
                csv.append(rec.getPosition()).append(",");
                csv.append(rec.getMusicalId()).append(",");
                csv.append("\"").append(escapeCsv(rec.getTitle())).append("\",");
                csv.append("\"").append(escapeCsv(rec.getGenre())).append("\",");
                csv.append("\"").append(escapeCsv(rec.getTheater())).append("\",");
                csv.append(String.format("%.4f", rec.getFinalScore())).append(",");
                csv.append(String.format("%.4f", rec.getStage1Score())).append(",");
                csv.append(String.format("%.4f", rec.getPctrScore())).append(",");
                csv.append(String.format("%.4f", rec.getContentCrossScore())).append(",");
                csv.append(String.format("%.4f", rec.getCfCrossScore())).append(",");
                csv.append("\"").append(escapeCsv(rec.getRecommendationReason())).append("\",");
                csv.append(rec.getSimilarityPercentage() != null ? 
                        String.format("%.1f%%", rec.getSimilarityPercentage()) : "N/A").append(",");
                csv.append(rec.getChartRanking() != null ? rec.getChartRanking() : "").append(",");
                csv.append(source).append(",");
                csv.append(rec.getAverageRating() != null ? rec.getAverageRating() : "").append(",");
                csv.append("\"").append(escapeCsv(rec.getPosterUrl())).append("\",");
                csv.append("\"").append(escapeCsv(rec.getStartDate())).append("\",");
                csv.append("\"").append(escapeCsv(rec.getEndDate())).append("\"\n");
            }
            
            // 파일명 생성
            String timestamp = LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("recommendations_user%d_%s.csv", userId, timestamp);
            
            // HTTP 헤더 설정
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", filename);
            
            log.info("CSV export completed for user {}: {} rows", userId, recommendations.size());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csv.toString());
            
        } catch (Exception e) {
            log.error("Failed to export CSV for user: {}", userId, e);
            return ResponseEntity.status(500)
                    .body("Error: " + e.getMessage());
        }
    }
    
    /**
     * 전체 사용자 추천 데이터 CSV 다운로드 API
     * GET /api/recommendations/export/csv/all-users
     * 
     * 모든 사용자의 추천 데이터를 하나의 CSV 파일로 다운로드
     */
    @GetMapping("/export/csv/all-users")
    public ResponseEntity<String> exportAllUsersToCsv(
            @RequestParam(defaultValue = "10") Integer limit) {
        
        try {
            List<User> users = userRepository.findAll().stream()
                    .limit(limit)
                    .collect(Collectors.toList());
            
            // CSV 생성
            StringBuilder csv = new StringBuilder();
            
            // UTF-8 BOM 추가
            csv.append("\uFEFF");
            
            // 헤더
            csv.append("사용자ID,사용자닉네임,순위,뮤지컬ID,제목,장르,극장,최종점수,");
            csv.append("추천이유,유사도,추천소스,평균평점\n");
            
            // 각 사용자별 추천 데이터
            for (User user : users) {
                try {
                    RecommendationRequest request = RecommendationRequest.builder()
                            .userId(user.getId())
                            .surface("home")
                            .count(17)
                            .build();
                    
                    List<RecommendationResponse> recommendations = recommendationService.recommend(request);
                    
                    for (RecommendationResponse rec : recommendations) {
                        // 추천 소스 판별
                        String source = "unknown";
                        if (rec.getRecommendationReason() != null) {
                            if (rec.getRecommendationReason().contains("음악 취향") && 
                                rec.getRecommendationReason().contains("뮤지컬 취향")) {
                                source = "통합알고리즘";
                            } else if (rec.getRecommendationReason().contains("음악 취향")) {
                                source = "음악취향전용";
                            } else if (rec.getRecommendationReason().contains("인기차트")) {
                                source = "인기차트전용";
                            }
                        }
                        
                        csv.append(user.getId()).append(",");
                        csv.append("\"").append(escapeCsv(user.getNickname())).append("\",");
                        csv.append(rec.getPosition()).append(",");
                        csv.append(rec.getMusicalId()).append(",");
                        csv.append("\"").append(escapeCsv(rec.getTitle())).append("\",");
                        csv.append("\"").append(escapeCsv(rec.getGenre())).append("\",");
                        csv.append("\"").append(escapeCsv(rec.getTheater())).append("\",");
                        csv.append(String.format("%.4f", rec.getFinalScore())).append(",");
                        csv.append("\"").append(escapeCsv(rec.getRecommendationReason())).append("\",");
                        csv.append(rec.getSimilarityPercentage() != null ? 
                                String.format("%.1f%%", rec.getSimilarityPercentage()) : "N/A").append(",");
                        csv.append(source).append(",");
                        csv.append(rec.getAverageRating() != null ? rec.getAverageRating() : "").append("\n");
                    }
                    
                } catch (Exception e) {
                    log.warn("Failed to export recommendations for user {}: {}", user.getId(), e.getMessage());
                }
            }
            
            // 파일명 생성
            String timestamp = LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("recommendations_all_users_%s.csv", timestamp);
            
            // HTTP 헤더 설정
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", filename);
            
            log.info("CSV export completed for {} users", users.size());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csv.toString());
            
        } catch (Exception e) {
            log.error("Failed to export CSV for all users", e);
            return ResponseEntity.status(500)
                    .body("Error: " + e.getMessage());
        }
    }
    
    /**
     * CSV 특수문자 이스케이프 헬퍼 메소드
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // 큰따옴표를 두 개로 변환 (CSV 표준)
        return value.replace("\"", "\"\"");
    }
    
    /**
     * Stage1 단계별 리스트 조회 API
     * GET /api/recommendations/stage1/{userId}
     * 
     * Stage1에서 생성되는 3가지 리스트를 반환:
     * - musicTasteList: 음악 취향 기반 추천
     * - musicalTasteList: 뮤지컬 취향 기반 추천
     * - popularityList: 인기차트 기반 추천
     */
    @GetMapping("/stage1/{userId}")
    public ResponseEntity<Stage1ListsResponse> getStage1Lists(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "KR") String region,
            @RequestParam(defaultValue = "300") Integer candidateCount) {
        
        try {
            // 사용자 확인
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("User not found: {}", userId);
                return ResponseEntity.badRequest().build();
            }
            
            // Stage1 후보 생성 (내부적으로 3가지 리스트가 생성됨)
            List<CandidateItem> candidates = stage1CandidateService.generateCandidates(
                    user, region, candidateCount);
            
            // 소스별로 분류
            List<CandidateItem> musicTasteItems = candidates.stream()
                    .filter(c -> "music_taste_only".equals(c.getSource()))
                    .collect(Collectors.toList());
            
            List<CandidateItem> popularityItems = candidates.stream()
                    .filter(c -> "popularity_only".equals(c.getSource()))
                    .collect(Collectors.toList());
            
            // 뮤지컬 취향 리스트는 로그 파일에서 읽어야 하므로, combined_algorithm으로 대체
            // 또는 별도 메서드로 생성 필요
            List<CandidateItem> musicalTasteItems = candidates.stream()
                    .filter(c -> "combined_algorithm".equals(c.getSource()))
                    .collect(Collectors.toList());
            
            // DTO 변환
            Stage1ListsResponse response = Stage1ListsResponse.builder()
                    .userId(userId)
                    .timestamp(LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .musicTasteList(convertToStage1Items(musicTasteItems))
                    .musicalTasteList(convertToStage1Items(musicalTasteItems))
                    .popularityList(convertToStage1Items(popularityItems))
                    .build();
            
            log.info("Stage1 lists generated for user {}: Music={}, Musical={}, Popularity={}",
                    userId, musicTasteItems.size(), musicalTasteItems.size(), popularityItems.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get Stage1 lists for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * CandidateItem을 Stage1Item DTO로 변환
     */
    private List<Stage1ListsResponse.Stage1Item> convertToStage1Items(List<CandidateItem> candidates) {
        return candidates.stream()
                .map(c -> Stage1ListsResponse.Stage1Item.builder()
                        .musicalId(c.getMusicalId())
                        .title(c.getTitle())
                        .source(c.getSource())
                        .stage1Score(c.getStage1Score())
                        .musicCfScore(c.getMusicCfScore())
                        .musicalCfScore(c.getMusicalCfScore())
                        .popularityScore(c.getPopularityScore())
                        .genre(c.getGenre())
                        .region(c.getRegion())
                        .isOnSale(c.getIsOnSale())
                        .isNew(c.getIsNew())
                        .build())
                .collect(Collectors.toList());
    }
}
