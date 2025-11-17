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

    // 기본 설정값
    private static final int DEFAULT_CANDIDATE_COUNT = 300; // Stage-1에서 생성할 후보 수
    private static final int DEFAULT_RECOMMENDATION_COUNT = 20; // 최종 추천 수

    /**
     * 메인 추천 API
     */
    @Transactional
    public List<RecommendationResponse> recommend(RecommendationRequest request) {
        long startTime = System.currentTimeMillis();
        String sessionId = generateSessionId(request);

        log.info("Starting recommendation for user: {}, surface: {}, count: {}",
                request.getUserId(), request.getSurface(), request.getCount());

        try {
            // 1. 사용자 조회
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getUserId()));

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

            // 8. 응답 DTO 변환
            List<RecommendationResponse> responses = convertToResponse(
                    finalCandidates, sessionId, request);

            // 9. 노출 로깅
            loggingService.logExposures(user, finalCandidates, request, sessionId);


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
     * 응답 DTO 변환
     */
    private List<RecommendationResponse> convertToResponse(List<CandidateItem> candidates,
                                                         String sessionId,
                                                         RecommendationRequest request) {
        List<RecommendationResponse> responses = new ArrayList<>();

        // Musical 정보를 한 번에 조회 (N+1 문제 방지)
        List<Long> musicalIds = candidates.stream()
                .map(CandidateItem::getMusicalId)
                .collect(Collectors.toList());

        Map<Long, Musical> musicalMap = musicalRepository.findAllById(musicalIds)
                .stream()
                .collect(Collectors.toMap(Musical::getId, m -> m));

        for (int i = 0; i < candidates.size(); i++) {
            CandidateItem candidate = candidates.get(i);
            Musical musical = musicalMap.get(candidate.getMusicalId());

            if (musical != null) {
                // 추천 이유 생성
                List<String> reasons = generateReasons(candidate);

                // 태그 파싱
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
                        .finalScore(candidate.getFinalScore())
                        .stage1Score(candidate.getStage1Score())
                        .pctrScore(candidate.getPctrScore())
                        .contentCrossScore(candidate.getContentCrossScore())
                        .cfCrossScore(candidate.getCfCrossScore())
                        .reasons(reasons)
                        .position(i + 1)
                        .recommendationId(sessionId)
                        .timestamp(System.currentTimeMillis())
                        .build();

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
}
