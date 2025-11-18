package com.melodical.backend.service.recommendation;

import com.melodical.backend.dto.*;
import com.melodical.backend.entity.*;
import com.melodical.backend.repository.*;
import com.melodical.backend.service.cache.RecommendationCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 메인 추천 서비스 - 전체 파이프라인 통합
 * Stage-1 → Stage-2 → Stage-3 → 로깅의 전체 플로우 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final Stage1CandidateService stage1Service;
    private final Stage2PCTRRankingService stage2Service;
    private final Stage3TwiddlerService stage3Service;
    private final RecommendationLoggingService loggingService;
    private final RecommendationCacheService cacheService;

    private final UserRepository userRepository;
    private final MusicalRepository musicalRepository;
    private final InteractionLogRepository interactionLogRepository;
    private final CrawledMusicalRankingRepository crawledMusicalRankingRepository;

    // 기본 설정값
    private static final int DEFAULT_CANDIDATE_COUNT = 300; // Stage-1에서 생성할 후보 수
    private static final int DEFAULT_RECOMMENDATION_COUNT = 20; // 최종 추천 수

    /**
     * 메인 추천 API
     */
    public List<RecommendationResponse> recommend(RecommendationRequest request) {
        long startTime = System.currentTimeMillis();
        String sessionId = generateSessionId(request);

        log.info("Starting recommendation for user: {}, surface: {}, count: {}",
                request.getUserId(), request.getSurface(), request.getCount());

        try {
            // 1. 사용자 조회
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getUserId()));

            // 2. 트랜잭션 내에서 추천 생성
            List<CandidateItem> finalCandidates = generateRecommendationsInternal(user, request);

            // 3. 응답 DTO 변환
            List<RecommendationResponse> responses = convertToResponse(
                    finalCandidates, sessionId, request);

            // 4. 트랜잭션 외부에서 로깅 (비동기)
            try {
                loggingService.logExposures(user, finalCandidates, request, sessionId);
            } catch (Exception e) {
                log.warn("Failed to log exposures, but continuing: {}", e.getMessage());
            }

            long endTime = System.currentTimeMillis();
            log.info("Recommendation completed for user: {} in {}ms, returned {} items",
                    request.getUserId(), endTime - startTime, responses.size());

            return responses;

        } catch (Exception e) {
            log.error("Recommendation failed for user: {}", request.getUserId(), e);
            // 폴백: 인기 기반 추천
            return getFallbackRecommendations(request, sessionId);
        }
    }

    /**
     * 추천 생성 내부 로직 (트랜잭션 적용)
     */
    @Transactional(readOnly = true)
    protected List<CandidateItem> generateRecommendationsInternal(User user, RecommendationRequest request) {
        // 2. 캐시 조회
        List<CandidateItem> cachedCandidates = cacheService.getCachedRecommendations(
                request.getUserId(), request.getSurface());

        List<CandidateItem> finalCandidates;

        if (cachedCandidates != null && !cachedCandidates.isEmpty()) {
            log.info("Using cached recommendations for user: {}", request.getUserId());
            finalCandidates = cachedCandidates;
        } else {
            // 3. Stage-1: 후보 생성 (Recall)
            List<CandidateItem> candidates = stage1Service.generateCandidates(
                    user, request.getRegion(), DEFAULT_CANDIDATE_COUNT);

            if (candidates.isEmpty()) {
                log.warn("No candidates generated for user: {}", request.getUserId());
                return Collections.emptyList();
            }

            // 4. Stage-2: pCTR 랭킹 (Precision)
            List<CandidateItem> rankedCandidates = stage2Service.rankCandidatesByPCTR(
                    user, candidates, request);

            // 5. Stage-3: Twiddler 후처리 (다양성/정책)
            finalCandidates = stage3Service.applyTwiddlerPolicies(
                    user, rankedCandidates, request);

            // 6. Redis 캐시 저장
            cacheService.cacheRecommendations(request.getUserId(), request.getSurface(), finalCandidates);

            // 7. Redis 업데이트
            stage3Service.updateSeenItems(user, finalCandidates);
            stage3Service.updateFrequencyCounters(user, finalCandidates);
        }

        return finalCandidates;
    }

    /**
     * 응답 DTO 변환
     * CrawledMusicalRanking 데이터 우선 사용 (평점 및 URL 정보 포함)
     */
    private List<RecommendationResponse> convertToResponse(List<CandidateItem> candidates,
                                                         String sessionId,
                                                         RecommendationRequest request) {
        List<RecommendationResponse> responses = new ArrayList<>();

        // Musical ID 리스트 추출
        List<Long> musicalIds = candidates.stream()
                .map(CandidateItem::getMusicalId)
                .collect(Collectors.toList());

        // Musical 데이터 조회 (제목으로 크롤링 데이터 매칭용)
        Map<Long, Musical> musicalMap = musicalRepository.findAllById(musicalIds)
                .stream()
                .collect(Collectors.toMap(Musical::getId, m -> m));

        // CrawledMusicalRanking 데이터 조회 (ID로 직접)
        Map<Long, CrawledMusicalRanking> crawledMap = crawledMusicalRankingRepository.findAllById(musicalIds)
                .stream()
                .collect(Collectors.toMap(CrawledMusicalRanking::getId, c -> c));
        
        // ID로 매칭되지 않은 경우를 위해 interparkId로도 매칭 시도
        List<CrawledMusicalRanking> allCrawledData = crawledMusicalRankingRepository.findLatestByRankingType("MONTHLY");
        Map<String, CrawledMusicalRanking> crawledByInterparkIdMap = new HashMap<>();
        
        for (CrawledMusicalRanking crawled : allCrawledData) {
            if (crawled.getInterparkId() != null) {
                crawledByInterparkIdMap.put(crawled.getInterparkId(), crawled);
            }
        }

        for (int i = 0; i < candidates.size(); i++) {
            CandidateItem candidate = candidates.get(i);
            Long musicalId = candidate.getMusicalId();
            
            // Musical 데이터 먼저 가져오기
            Musical musicalData = musicalMap.get(musicalId);
            
            // CrawledMusicalRanking 데이터 찾기 (여러 방법 시도)
            CrawledMusicalRanking crawledData = crawledMap.get(musicalId);
            
            // ID로 매칭 안 되면 interparkId로 매칭 시도
            if (crawledData == null && musicalData != null && musicalData.getInterparkId() != null) {
                crawledData = crawledByInterparkIdMap.get(musicalData.getInterparkId());
            }

            // 데이터가 하나라도 있으면 응답 생성
            if (crawledData != null || musicalData != null) {
                // 추천 이유 생성
                List<String> reasons = generateReasons(candidate);

                // 태그 파싱 (crawledData 우선, 없으면 musicalData)
                String tagsString = crawledData != null ? crawledData.getGenre() : 
                                   (musicalData != null ? musicalData.getTags() : null);
                List<String> tags = tagsString != null ?
                        Arrays.asList(tagsString.split(",")) :
                        Collections.emptyList();

                RecommendationResponse.RecommendationResponseBuilder builder = RecommendationResponse.builder()
                        .musicalId(musicalId)
                        .finalScore(candidate.getFinalScore())
                        .stage1Score(candidate.getStage1Score())
                        .pctrScore(candidate.getPctrScore())
                        .contentCrossScore(candidate.getContentCrossScore())
                        .cfCrossScore(candidate.getCfCrossScore())
                        .reasons(reasons)
                        .position(i + 1)
                        .recommendationReason(candidate.getRecommendationReason())
                        .similarityPercentage(candidate.getSimilarityPercentage())
                        .chartRanking(candidate.getChartRanking())
                        .recommendationId(sessionId)
                        .timestamp(System.currentTimeMillis());

                // CrawledMusicalRanking 데이터 우선 적용
                if (crawledData != null) {
                    builder
                        .title(crawledData.getTitle())
                        .posterUrl(crawledData.getPosterUrl())
                        .theater(crawledData.getTheaterName())
                        .genre(crawledData.getGenre())
                        .isOnSale(crawledData.getIsAvailable() != null ? crawledData.getIsAvailable() : true)
                        .isNew(false)
                        .averageRating(crawledData.getAverageRating())  // 평점 추가
                        .interparkUrl(crawledData.getInterparkUrl())    // URL 추가
                        .yes24Url(crawledData.getYes24Url());          // URL 추가
                    
                    // performancePeriod 파싱
                    if (crawledData.getPerformancePeriod() != null) {
                        String[] dates = crawledData.getPerformancePeriod().split("~");
                        if (dates.length >= 1) builder.startDate(dates[0].trim());
                        if (dates.length >= 2) builder.endDate(dates[1].trim());
                    }
                } 
                // Fallback: Musical 데이터 사용
                else if (musicalData != null) {
                    builder
                        .title(musicalData.getTitle())
                        .posterUrl(musicalData.getPosterUrl())
                        .theater(musicalData.getTheater())
                        .startDate(musicalData.getStartDate())
                        .endDate(musicalData.getEndDate())
                        .genre(musicalData.getGenre())
                        .interparkUrl(musicalData.getInterparkUrl())  // Musical에서도 URL 가져오기
                        .yes24Url(musicalData.getYes24Url())          // Musical에서도 URL 가져오기
                        .region(musicalData.getRegion())
                        .priceMin(musicalData.getPriceMin())
                        .priceMax(musicalData.getPriceMax())
                        .isOnSale(musicalData.getIsOnSale())
                        .isNew(musicalData.getIsNew())
                        .popularityScore(musicalData.getPopularityScore());
                }

                builder.tags(tags);
                
                // posterUrl이 없으면 URL에서 생성 시도
                RecommendationResponse response = builder.build();
                if (response.getPosterUrl() == null || response.getPosterUrl().trim().isEmpty()) {
                    String generatedPosterUrl = generatePosterUrlFromTicketUrl(
                            response.getInterparkUrl(), 
                            response.getYes24Url()
                    );
                    if (generatedPosterUrl != null) {
                        response.setPosterUrl(generatedPosterUrl);
                    }
                }
                
                responses.add(response);
            }
        }

        return responses;
    }

    /**
     * 추천 이유 생성
     */
    private List<String> generateReasons(CandidateItem candidate) {
        List<String> reasons = new ArrayList<>();

        // Stage-1 점수 기반 이유
        if (candidate.getContentCrossScore() > 0.7) {
            reasons.add("음악 취향과 잘 맞는 작품");
        }

        if (candidate.getCfCrossScore() > 0.6) {
            reasons.add("비슷한 취향 사용자들의 선택");
        }

        // 메타데이터 기반 이유
        if (Boolean.TRUE.equals(candidate.getIsNew())) {
            reasons.add("최신 작품");
        }

        if (Boolean.TRUE.equals(candidate.getIsOnSale())) {
            reasons.add("현재 예매 가능");
        }

        if (candidate.getPopularityScore() != null && candidate.getPopularityScore() > 0.8) {
            reasons.add("인기 상승 작품");
        }

        // 소스별 이유
        if ("popularity".equals(candidate.getSource())) {
            reasons.add("많은 사용자가 관심을 가진 작품");
        }

        return reasons.isEmpty() ? List.of("추천 작품") : reasons;
    }

    /**
     * 폴백 추천 (시스템 오류 시)
     */
    private List<RecommendationResponse> getFallbackRecommendations(RecommendationRequest request,
                                                                  String sessionId) {
        log.info("Generating fallback recommendations for user: {}", request.getUserId());

        try {
            // 인기 뮤지컬 기반 폴백
            List<Musical> popularMusicals = musicalRepository.findAll().stream()
                    .filter(m -> Boolean.TRUE.equals(m.getIsOnSale())) // 판매중만
                    .sorted((a, b) -> Double.compare(
                            b.getPopularityScore() != null ? b.getPopularityScore() : 0.0,
                            a.getPopularityScore() != null ? a.getPopularityScore() : 0.0))
                    .limit(request.getCount())
                    .collect(Collectors.toList());

            List<RecommendationResponse> fallbackResponses = new ArrayList<>();

            for (int i = 0; i < popularMusicals.size(); i++) {
                Musical musical = popularMusicals.get(i);

                List<String> tags = musical.getTags() != null ?
                        Arrays.asList(musical.getTags().split(",")) :
                        Collections.emptyList();

                RecommendationResponse response = RecommendationResponse.builder()
                        .musicalId(musical.getId())
                        .title(musical.getTitle())
                        .posterUrl(musical.getPosterUrl())
                        .theater(musical.getTheater())
                        .startDate(musical.getStartDate())
                        .endDate(musical.getEndDate())
                        .genre(musical.getGenre())
                        .tags(tags)
                        .region(musical.getRegion())
                        .priceMin(musical.getPriceMin())
                        .priceMax(musical.getPriceMax())
                        .isOnSale(musical.getIsOnSale())
                        .isNew(musical.getIsNew())
                        .popularityScore(musical.getPopularityScore())
                        .finalScore(musical.getPopularityScore() != null ? musical.getPopularityScore() : 0.0)
                        .stage1Score(0.0)
                        .pctrScore(0.0)
                        .contentCrossScore(0.0)
                        .cfCrossScore(0.0)
                        .reasons(List.of("인기 작품", "폴백 추천"))
                        .position(i + 1)
                        .recommendationId(sessionId)
                        .timestamp(System.currentTimeMillis())
                        .build();

                fallbackResponses.add(response);
            }

            return fallbackResponses;

        } catch (Exception e) {
            log.error("Fallback recommendation also failed", e);
            return Collections.emptyList();
        }
    }

    /**
     * 세션 ID 생성
     */
    private String generateSessionId(RecommendationRequest request) {
        return String.format("rec_%d_%s_%d",
                request.getUserId(),
                request.getSurface(),
                System.currentTimeMillis());
    }

    /**
     * 추천 품질 메트릭 조회 (모니터링용)
     */
    public Map<String, Object> getRecommendationMetrics(Long userId, int days) {
        Map<String, Object> metrics = new HashMap<>();

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusDays(days);

            // 기본 메트릭 수집
            metrics.put("userId", userId);
            metrics.put("period", days + " days");
            metrics.put("startTime", startTime);
            metrics.put("endTime", endTime);

            // 사용자 상호작용 통계
            List<InteractionLog> interactions = interactionLogRepository
                    .findByUserAndInteractionAtBetween(user, startTime, endTime);

            long totalInteractions = interactions.size();
            long clickCount = interactions.stream()
                    .filter(log -> "click".equals(log.getInteractionType()))
                    .count();
            long ratingCount = interactions.stream()
                    .filter(log -> "rating".equals(log.getInteractionType()))
                    .count();

            metrics.put("totalInteractions", totalInteractions);
            metrics.put("clickCount", clickCount);
            metrics.put("ratingCount", ratingCount);

            // 평균 평점
            OptionalDouble avgRating = interactions.stream()
                    .filter(log -> "rating".equals(log.getInteractionType()) && log.getRatingValue() != null)
                    .mapToDouble(InteractionLog::getRatingValue)
                    .average();

            metrics.put("averageRating", avgRating.isPresent() ? avgRating.getAsDouble() : 0.0);

            // 선호 장르 분석
            Map<String, Long> genreDistribution = interactions.stream()
                    .filter(log -> log.getMusical() != null && log.getMusical().getGenre() != null)
                    .collect(Collectors.groupingBy(
                            log -> log.getMusical().getGenre(),
                            Collectors.counting()
                    ));

            metrics.put("genreDistribution", genreDistribution);

            return metrics;

        } catch (Exception e) {
            log.error("Failed to collect metrics for user: {}", userId, e);
            metrics.put("error", e.getMessage());
            return metrics;
        }
    }
    
    /**
     * 티켓 URL로부터 포스터 URL 생성
     * Interpark: https://tickets.interpark.com/goods/25004474 
     *   -> https://ticketimage.interpark.com/Play/image/large/25/25004474_p.gif
     * Yes24: 현재는 지원하지 않음 (크롤링 필요)
     */
    private String generatePosterUrlFromTicketUrl(String interparkUrl, String yes24Url) {
        // Interpark URL에서 poster URL 생성
        if (interparkUrl != null && interparkUrl.contains("interpark.com/goods/")) {
            try {
                // URL에서 굿즈 ID 추출 (예: 25004474)
                String[] parts = interparkUrl.split("/goods/");
                if (parts.length == 2) {
                    String goodsId = parts[1].trim();
                    // ID의 앞 2자리 추출 (예: 25004474 -> 25)
                    String prefix = goodsId.substring(0, 2);
                    // Poster URL 생성
                    return String.format("https://ticketimage.interpark.com/Play/image/large/%s/%s_p.gif", 
                                        prefix, goodsId);
                }
            } catch (Exception e) {
                log.warn("Failed to generate poster URL from Interpark URL: {}", interparkUrl, e);
            }
        }
        
        // Yes24는 현재 지원하지 않음 (향후 크롤러 개선 필요)
        
        return null;
    }
}
