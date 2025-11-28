package com.melodical.backend.service.recommendation;

import com.melodical.backend.dto.CandidateItem;
import com.melodical.backend.entity.*;
import com.melodical.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Stage-1: 후보 생성 서비스 (Recall 최적화)
 * 평점 기반 협업 필터링으로 Top-M 후보를 수집
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Stage1CandidateService {

    private final MusicalRepository musicalRepository;
    private final UserRepository userRepository;
    private final RatingBasedSimilarityService ratingBasedSimilarityService;
    private final FavoriteRepository favoriteRepository;
    private final CrawledMusicalRankingRepository crawledRankingRepository;
    private final com.melodical.backend.service.recommendation.RecommendationStageLogger recommendationStageLogger;

    // ✨ 새로운 가중치 설정 (음악 CF 30% + 뮤지컬 CF 50% + 인기차트 20%)
    private static final double MUSIC_CF_WEIGHT = 0.30;     // 음악 취향 기반 협업 필터링
    private static final double MUSICAL_CF_WEIGHT = 0.50;   // 뮤지컬 취향 기반 협업 필터링
    private static final double POPULARITY_WEIGHT = 0.20;   // 인기차트 기반
    
    // ✨ 추천 구성 비율 설정
    private static final int COMBINED_ALGORITHM_COUNT = 7;  // 통합 알고리즘 추천 수
    private static final int MUSIC_TASTE_ONLY_COUNT = 5;    // 음악 취향만 기반 추천 수
    private static final int POPULARITY_ONLY_COUNT = 5;     // 인기 차트만 기반 추천 수

    /**
     * ✨ 새로운 추천 알고리즘: 평점 기반 협업 필터링
     * - 통합 알고리즘 (음악 30% + 뮤지컬 50% + 인기 20%): 7개
     * - 음악 취향만 기반 추천: 5개
     * - 인기 차트만 기반 추천: 5개
     * 총 17개 추천 (중복 제거, 평가한 작품 및 관심 표시 제외)
     */
    public List<CandidateItem> generateCandidates(User user, String region, int topM) {
        log.info("🎯 Generating candidates for user: {} using rating-based CF", user.getId());

        // 사용자가 이미 평가한 뮤지컬 제외
        Set<String> userRatedMusicals = ratingBasedSimilarityService.getUserRatedMusicalIds(user.getId());
        log.info("  User has rated {} musicals", userRatedMusicals.size());
        
        // 사용자가 관심 표시한 뮤지컬 제외
        Set<Long> userFavoriteMusicalIds = favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(fav -> fav.getMusical().getId())
                .collect(Collectors.toSet());
        log.info("  User has {} favorite musicals", userFavoriteMusicalIds.size());
        
        // Favorite의 Interpark ID도 제외 목록에 추가
        Set<String> userFavoriteInterparkIds = favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(fav -> fav.getMusical().getInterparkId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 모든 활성 사용자 목록 조회 (평점을 남긴 사용자)
        List<Long> candidateUserIds = userRepository.findAll().stream()
                .map(User::getId)
                .collect(Collectors.toList());

        // 1) 음악 취향 기반 유사 사용자 찾기 (30%)
        Map<Long, Double> musicSimilarUsers = ratingBasedSimilarityService
                .findSimilarUsersByMusicTaste(user.getId(), candidateUserIds, 20);
        log.info("  Found {} similar users by music taste", musicSimilarUsers.size());

        // 2) 뮤지컬 취향 기반 유사 사용자 찾기 (50%)
        Map<Long, Double> musicalSimilarUsers = ratingBasedSimilarityService
                .findSimilarUsersByMusicalTaste(user.getId(), candidateUserIds, 20);
        log.info("  Found {} similar users by musical taste", musicalSimilarUsers.size());

        // Cold start 처리
        if (musicSimilarUsers.isEmpty() && musicalSimilarUsers.isEmpty()) {
            log.info("  ⚠️ Cold start user - using popularity-based recommendations");
            return handleColdStartUser(user, region, topM);
        }

        // 3) 음악 취향 기반 뮤지컬 점수 계산
        Map<String, Double> musicBasedScores = ratingBasedSimilarityService
                .getWeightedMusicalScores(musicSimilarUsers, userRatedMusicals);
        log.info("  Music-based CF found {} musicals", musicBasedScores.size());

        // 4) 뮤지컬 취향 기반 뮤지컬 점수 계산
        Map<String, Double> musicalBasedScores = ratingBasedSimilarityService
                .getWeightedMusicalScores(musicalSimilarUsers, userRatedMusicals);
        log.info("  Musical-based CF found {} musicals", musicalBasedScores.size());

        // 5) 인기 차트 점수
        Map<Long, Double> popularityScores = getPopularityScores();

        // ========================================
        // 6-1) 통합 알고리즘 (음악 30% + 뮤지컬 50% + 인기 20%) - 7개
        // ========================================
        List<CandidateItem> combinedCandidates = generateCombinedAlgorithmCandidates(
                musicBasedScores, musicalBasedScores, popularityScores, 
                userRatedMusicals, userFavoriteInterparkIds, userFavoriteMusicalIds,
                COMBINED_ALGORITHM_COUNT);
        
        // ========================================
        // 6-2) 음악 취향만 기반 추천 - 5개 (중복 제외)
        // ========================================
        Set<String> usedInterparkIds = combinedCandidates.stream()
                .map(c -> findMusicalByInterparkId(c.getMusicalId())
                        .map(Musical::getInterparkId)
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Set<Long> usedMusicalIds = combinedCandidates.stream()
                .map(CandidateItem::getMusicalId)
                .collect(Collectors.toSet());
        
        List<CandidateItem> musicOnlyCandidates = generateMusicTasteOnlyCandidates(
                musicBasedScores, popularityScores,
                userRatedMusicals, userFavoriteInterparkIds, userFavoriteMusicalIds,
                usedInterparkIds, usedMusicalIds,
                MUSIC_TASTE_ONLY_COUNT);
        
        // ========================================
        // 6-3) 인기 차트만 기반 추천 - 5개 (중복 제외)
        // ========================================
        usedInterparkIds.addAll(musicOnlyCandidates.stream()
                .map(c -> findMusicalByInterparkId(c.getMusicalId())
                        .map(Musical::getInterparkId)
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        
        usedMusicalIds.addAll(musicOnlyCandidates.stream()
                .map(CandidateItem::getMusicalId)
                .collect(Collectors.toSet()));
        
        List<CandidateItem> popularityOnlyCandidates = generatePopularityOnlyCandidates(
                popularityScores,
                userRatedMusicals, userFavoriteInterparkIds, userFavoriteMusicalIds,
                usedInterparkIds, usedMusicalIds,
                POPULARITY_ONLY_COUNT);

        // ----- 추가: 뮤지컬 기반 리스트 별도 생성 (로깅용) -----
        List<CandidateItem> musicalOnlyCandidates = generateMusicalTasteOnlyCandidates(
                musicalBasedScores, popularityScores,
                userRatedMusicals, userFavoriteInterparkIds, userFavoriteMusicalIds,
                usedInterparkIds, usedMusicalIds,
                COMBINED_ALGORITHM_COUNT);

        // 로그 파일로 Stage-1의 세 가지 리스트를 남김
        try {
            recommendationStageLogger.logStage1Lists(user, musicOnlyCandidates, musicalOnlyCandidates, popularityOnlyCandidates);
        } catch (Exception e) {
            log.warn("Failed to write stage1 lists to file", e);
        }
        
        // ========================================
        // 7) 최종 후보 리스트 통합
        // ========================================
        List<CandidateItem> finalCandidates = new ArrayList<>();
        finalCandidates.addAll(combinedCandidates);
        finalCandidates.addAll(musicOnlyCandidates);
        finalCandidates.addAll(popularityOnlyCandidates);

        log.info("✅ Generated {} candidates (Combined: {}, Music-only: {}, Popularity-only: {})",
                finalCandidates.size(),
                combinedCandidates.size(),
                musicOnlyCandidates.size(),
                popularityOnlyCandidates.size());

        return finalCandidates;
    }
    
    /**
     * 통합 알고리즘 후보 생성 (음악 30% + 뮤지컬 50% + 인기 20%)
     */
    private List<CandidateItem> generateCombinedAlgorithmCandidates(
            Map<String, Double> musicBasedScores,
            Map<String, Double> musicalBasedScores,
            Map<Long, Double> popularityScores,
            Set<String> userRatedMusicals,
            Set<String> userFavoriteInterparkIds,
            Set<Long> userFavoriteMusicalIds,
            int count) {
        
        Map<String, CandidateItem> candidateMap = new HashMap<>();
        
        // Chart rankings 조회
        Map<Long, Integer> chartRankings = getChartRankings();
        
        // 음악 기반 점수 추가 (30%)
        for (Map.Entry<String, Double> entry : musicBasedScores.entrySet()) {
            String musicalId = entry.getKey();
            double score = entry.getValue() * MUSIC_CF_WEIGHT;
            
            Optional<Musical> musicalOpt = findMusicalByInterparkId(musicalId);
            if (musicalOpt.isEmpty()) continue;
            
            Musical musical = musicalOpt.get();
            
            // 제외 조건 확인
            if (userFavoriteInterparkIds.contains(musical.getInterparkId()) ||
                userFavoriteMusicalIds.contains(musical.getId())) {
                continue;
            }
            
            CandidateItem candidate = CandidateItem.builder()
                    .musicalId(musical.getId())
                    .title(musical.getTitle())
                    .contentCrossScore(0.0)
                    .cfCrossScore(score)
                    .sideScore(0.0)
                    .genre(musical.getGenre())
                    .region(musical.getRegion())
                    .isOnSale(musical.getIsOnSale())
                    .isNew(musical.getIsNew())
                    .popularityScore(popularityScores.getOrDefault(musical.getId(), 0.0))
                    .source("combined_algorithm")
                    .musicCfScore(score)  // 음악 CF 점수
                    .musicalCfScore(0.0)  // 뮤지컬 CF 점수
                    .build();
            candidateMap.put(musicalId, candidate);
        }

        // 뮤지컬 기반 점수 추가 (50%)
        for (Map.Entry<String, Double> entry : musicalBasedScores.entrySet()) {
            String musicalId = entry.getKey();
            double score = entry.getValue() * MUSICAL_CF_WEIGHT;
            
            if (candidateMap.containsKey(musicalId)) {
                // 기존 후보에 뮤지컬 CF 점수 추가
                CandidateItem existing = candidateMap.get(musicalId);
                existing.setMusicalCfScore(score);
                existing.setCfCrossScore(existing.getCfCrossScore() + score);
            } else {
                // 새 후보 생성
                Optional<Musical> musicalOpt = findMusicalByInterparkId(musicalId);
                if (musicalOpt.isEmpty()) continue;
                
                Musical musical = musicalOpt.get();
                
                // 제외 조건 확인
                if (userFavoriteInterparkIds.contains(musical.getInterparkId()) ||
                    userFavoriteMusicalIds.contains(musical.getId())) {
                    continue;
                }
                
                CandidateItem candidate = CandidateItem.builder()
                        .musicalId(musical.getId())
                        .title(musical.getTitle())
                        .contentCrossScore(0.0)
                        .cfCrossScore(score)
                        .sideScore(0.0)
                        .genre(musical.getGenre())
                        .region(musical.getRegion())
                        .isOnSale(musical.getIsOnSale())
                        .isNew(musical.getIsNew())
                        .popularityScore(popularityScores.getOrDefault(musical.getId(), 0.0))
                        .source("combined_algorithm")
                        .musicCfScore(0.0)
                        .musicalCfScore(score)
                        .build();
                candidateMap.put(musicalId, candidate);
            }
        }

        // 인기도 점수 추가 (20%)
        for (CandidateItem candidate : candidateMap.values()) {
            double popularityScore = popularityScores.getOrDefault(candidate.getMusicalId(), 0.0);
            double popularityContribution = popularityScore * POPULARITY_WEIGHT;
            candidate.setPopularityScore(popularityScore);
            candidate.setChartRanking(chartRankings.get(candidate.getMusicalId()));  // 실제 ranking 설정
            candidate.setCfCrossScore(candidate.getCfCrossScore() + popularityContribution);
        }

        // 최종 점수 계산 및 정렬
        return candidateMap.values().stream()
                .peek(item -> {
                    // Stage1 점수 = CF 점수 (이미 가중치 적용됨)
                    item.setStage1Score(item.getCfCrossScore());
                    
                    // 추천 이유 설정
                    setRecommendationReason(item);
                })
                .sorted((a, b) -> Double.compare(b.getStage1Score(), a.getStage1Score()))
                .limit(count)
                .collect(Collectors.toList());
    }
    
    /**
     * 음악 취향만 기반 추천 생성
     */
    private List<CandidateItem> generateMusicTasteOnlyCandidates(
            Map<String, Double> musicBasedScores,
            Map<Long, Double> popularityScores,
            Set<String> userRatedMusicals,
            Set<String> userFavoriteInterparkIds,
            Set<Long> userFavoriteMusicalIds,
            Set<String> usedInterparkIds,
            Set<Long> usedMusicalIds,
            int count) {
        
        List<CandidateItem> candidates = new ArrayList<>();
        
        // Chart rankings 조회
        Map<Long, Integer> chartRankings = getChartRankings();
        
        // 음악 취향 기반 점수 내림차순 정렬
        List<Map.Entry<String, Double>> sortedScores = musicBasedScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());
        
        for (Map.Entry<String, Double> entry : sortedScores) {
            if (candidates.size() >= count) break;
            
            String interparkId = entry.getKey();
            double avgRating = entry.getValue(); // 가중 평균 평점 (0~5 범위)
            
            // 중복 제외
            if (usedInterparkIds.contains(interparkId)) continue;
            
            Optional<Musical> musicalOpt = findMusicalByInterparkId(interparkId);
            if (musicalOpt.isEmpty()) continue;
            
            Musical musical = musicalOpt.get();
            
            // 제외 조건 확인
            if (usedMusicalIds.contains(musical.getId()) ||
                userFavoriteInterparkIds.contains(interparkId) ||
                userFavoriteMusicalIds.contains(musical.getId())) {
                continue;
            }
            
            // 음악 취향만 기반 점수 (100%)
            double score = avgRating / 5.0; // 0~1 범위로 정규화
            
            CandidateItem candidate = CandidateItem.builder()
                    .musicalId(musical.getId())
                    .title(musical.getTitle())
                    .contentCrossScore(0.0)
                    .cfCrossScore(score)
                    .sideScore(0.0)
                    .stage1Score(score)
                    .genre(musical.getGenre())
                    .region(musical.getRegion())
                    .isOnSale(musical.getIsOnSale())
                    .isNew(musical.getIsNew())
                    .popularityScore(popularityScores.getOrDefault(musical.getId(), 0.0))
                    .chartRanking(chartRankings.get(musical.getId()))  // 실제 ranking 설정
                    .source("music_taste_only")
                    .musicCfScore(score)
                    .musicalCfScore(0.0)
                    .build();
            
            // 추천 이유 설정
            setRecommendationReasonForMusicOnly(candidate, avgRating);
            
            candidates.add(candidate);
        }
        
        log.info("  Generated {} music-taste-only candidates", candidates.size());
        return candidates;
    }

    /**
     * 뮤지컬 취향만 기반 추천 리스트 (로깅/디버깅 용)
     */
    private List<CandidateItem> generateMusicalTasteOnlyCandidates(
            Map<String, Double> musicalBasedScores,
            Map<Long, Double> popularityScores,
            Set<String> userRatedMusicals,
            Set<String> userFavoriteInterparkIds,
            Set<Long> userFavoriteMusicalIds,
            Set<String> usedInterparkIds,
            Set<Long> usedMusicalIds,
            int count) {

        List<CandidateItem> candidates = new ArrayList<>();

        // Chart rankings 조회
        Map<Long, Integer> chartRankings = getChartRankings();

        // 뮤지컬 취향 기반 점수 내림차순 정렬
        List<Map.Entry<String, Double>> sortedScores = musicalBasedScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        for (Map.Entry<String, Double> entry : sortedScores) {
            if (candidates.size() >= count) break;

            String interparkId = entry.getKey();
            double avgRating = entry.getValue();

            // 중복 제외
            if (usedInterparkIds.contains(interparkId)) continue;

            Optional<Musical> musicalOpt = findMusicalByInterparkId(interparkId);
            if (musicalOpt.isEmpty()) continue;

            Musical musical = musicalOpt.get();

            // 제외 조건 확인
            if (usedMusicalIds.contains(musical.getId()) ||
                userFavoriteInterparkIds.contains(interparkId) ||
                userFavoriteMusicalIds.contains(musical.getId())) {
                continue;
            }

            // 뮤지컬 취향만 기반 점수 (100%)
            double score = avgRating / 5.0; // 0~1 정규화

            CandidateItem candidate = CandidateItem.builder()
                    .musicalId(musical.getId())
                    .title(musical.getTitle())
                    .contentCrossScore(0.0)
                    .cfCrossScore(score)
                    .sideScore(0.0)
                    .stage1Score(score)
                    .genre(musical.getGenre())
                    .region(musical.getRegion())
                    .isOnSale(musical.getIsOnSale())
                    .isNew(musical.getIsNew())
                    .popularityScore(popularityScores.getOrDefault(musical.getId(), 0.0))
                    .chartRanking(chartRankings.get(musical.getId()))  // 실제 ranking 설정
                    .source("musical_taste_only")
                    .musicCfScore(0.0)
                    .musicalCfScore(score)
                    .build();

            // 추천 이유 설정
            setRecommendationReason(candidate);

            candidates.add(candidate);
        }

        log.info("  Generated {} musical-taste-only candidates (for logging)", candidates.size());
        return candidates;
    }
    
    /**
     * 인기 차트만 기반 추천 생성
     */
    private List<CandidateItem> generatePopularityOnlyCandidates(
            Map<Long, Double> popularityScores,
            Set<String> userRatedMusicals,
            Set<String> userFavoriteInterparkIds,
            Set<Long> userFavoriteMusicalIds,
            Set<String> usedInterparkIds,
            Set<Long> usedMusicalIds,
            int count) {
        
        List<CandidateItem> candidates = new ArrayList<>();
        
        // Chart rankings 조회
        Map<Long, Integer> chartRankings = getChartRankings();
        
        // 인기도 점수 내림차순 정렬
        List<Map.Entry<Long, Double>> sortedScores = popularityScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .collect(Collectors.toList());
        
        for (Map.Entry<Long, Double> entry : sortedScores) {
            if (candidates.size() >= count) break;
            
            Long musicalId = entry.getKey();
            if (musicalId == null) continue;
            
            double popularityScore = entry.getValue();
            
            // 중복 제외
            if (usedMusicalIds.contains(musicalId)) continue;
            
            Optional<Musical> musicalOpt = musicalRepository.findById(musicalId);
            if (musicalOpt.isEmpty()) continue;
            
            Musical musical = musicalOpt.get();
            
            // 제외 조건 확인
            if (userRatedMusicals.contains(musical.getInterparkId()) ||
                usedInterparkIds.contains(musical.getInterparkId()) ||
                userFavoriteInterparkIds.contains(musical.getInterparkId()) ||
                userFavoriteMusicalIds.contains(musicalId)) {
                continue;
            }
            
            CandidateItem candidate = CandidateItem.builder()
                    .musicalId(musical.getId())
                    .title(musical.getTitle())
                    .contentCrossScore(0.0)
                    .cfCrossScore(0.0)
                    .sideScore(popularityScore)
                    .stage1Score(popularityScore)
                    .genre(musical.getGenre())
                    .region(musical.getRegion())
                    .isOnSale(musical.getIsOnSale())
                    .isNew(musical.getIsNew())
                    .popularityScore(popularityScore)
                    .chartRanking(chartRankings.get(musicalId))  // 실제 ranking 설정
                    .source("popularity_only")
                    .musicCfScore(0.0)
                    .musicalCfScore(0.0)
                    .build();
            
            // 추천 이유 설정
            setRecommendationReasonForPopularityOnly(candidate);
            
            candidates.add(candidate);
        }
        
        log.info("  Generated {} popularity-only candidates", candidates.size());
        return candidates;
    }
    
    /**
     * Musical ID로 Musical 찾기 (Overloaded for Long)
     */
    private Optional<Musical> findMusicalByInterparkId(Long musicalId) {
        if (musicalId == null) return Optional.empty();
        return musicalRepository.findById(musicalId);
    }
    
    /**
     * Interpark ID로 Musical 찾기 (Overloaded for String)
     */
    private Optional<Musical> findMusicalByInterparkId(String interparkId) {
        if (interparkId == null) return Optional.empty();
        return musicalRepository.findAll().stream()
                .filter(m -> interparkId.equals(m.getInterparkId()))
                .findFirst();
    }

    /**
     * 인기도 점수 계산 (CrawledMusicalRanking의 combinedRank 기반)
     * - 순위가 낮을수록 인기가 높음 (1위 > 2위 > 3위 ...)
     * - 점수 = (maxRank - rank + 1) / maxRank로 정규화 (0~1 범위)
     */
    private Map<Long, Double> getPopularityScores() {
        // 최신 월간 랭킹 데이터 조회
        List<CrawledMusicalRanking> rankings = crawledRankingRepository.findLatestByRankingType("MONTHLY");
        
        if (rankings.isEmpty()) {
            log.warn("⚠️ No ranking data found. Returning empty popularity scores.");
            return new HashMap<>();
        }

        log.info("📊 Loading popularity scores from {} rankings", rankings.size());

        // interparkId -> Musical 매핑
        Map<String, Musical> interparkIdToMusical = musicalRepository.findAll().stream()
                .filter(m -> m.getInterparkId() != null)
                .collect(Collectors.toMap(Musical::getInterparkId, m -> m, (m1, m2) -> m1));

        // 최대 순위 계산 (정규화를 위해)
        int maxRank = rankings.stream()
                .mapToInt(r -> r.getCombinedRank() != null ? r.getCombinedRank() : 0)
                .max().orElse(100);

        Map<Long, Double> scores = new HashMap<>();
        
        for (CrawledMusicalRanking ranking : rankings) {
            if (ranking.getInterparkId() == null || ranking.getCombinedRank() == null) {
                continue;
            }

            Musical musical = interparkIdToMusical.get(ranking.getInterparkId());
            if (musical != null) {
                // 순위를 점수로 변환: 1위 = 1.0, 최하위 = ~0.0
                double score = (double) (maxRank - ranking.getCombinedRank() + 1) / maxRank;
                scores.put(musical.getId(), score);
            }
        }

        log.info("✅ Loaded popularity scores for {} musicals (maxRank: {})", scores.size(), maxRank);
        return scores;
    }

    /**
     * Musical ID -> Chart Ranking 매핑 생성
     * CrawledMusicalRanking의 combinedRank를 사용
     */
    private Map<Long, Integer> getChartRankings() {
        List<CrawledMusicalRanking> rankings = crawledRankingRepository.findLatestByRankingType("MONTHLY");
        
        if (rankings.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Musical> interparkIdToMusical = musicalRepository.findAll().stream()
                .filter(m -> m.getInterparkId() != null)
                .collect(Collectors.toMap(Musical::getInterparkId, m -> m, (m1, m2) -> m1));

        Map<Long, Integer> chartRankings = new HashMap<>();
        
        for (CrawledMusicalRanking ranking : rankings) {
            if (ranking.getInterparkId() == null || ranking.getCombinedRank() == null) {
                continue;
            }

            Musical musical = interparkIdToMusical.get(ranking.getInterparkId());
            if (musical != null) {
                chartRankings.put(musical.getId(), ranking.getCombinedRank());
            }
        }

        return chartRankings;
    }

    /**
     * ✨ 새로운 추천 이유 설정 (평점 기반 CF)
     * 가장 높은 점수를 기준으로 추천 이유를 결정
     */
    private void setRecommendationReason(CandidateItem item) {
        double musicCfScore = item.getMusicCfScore() != null ? item.getMusicCfScore() : 0.0;
        double musicalCfScore = item.getMusicalCfScore() != null ? item.getMusicalCfScore() : 0.0;
        double popularityScore = item.getPopularityScore() != null ? item.getPopularityScore() : 0.0;
        
        // 각 요소의 기여도 계산
        // musicCfScore와 musicalCfScore는 이미 가중치가 적용된 값 (score * WEIGHT)
        double musicContribution = musicCfScore;
        double musicalContribution = musicalCfScore;
        // popularityScore는 가중치가 적용되지 않은 값이므로 가중치 적용
        double popularityContribution = popularityScore * POPULARITY_WEIGHT;
        
        log.info("🎯 Setting recommendation reason for '{}' (ID: {})", item.getTitle(), item.getMusicalId());
        log.info("  📊 Scores:");
        log.info("    - Music CF (음악 취향 CF): {} (weight: {}%) → contribution: {}", 
                 musicCfScore, (int)(MUSIC_CF_WEIGHT * 100), musicContribution);
        log.info("    - Musical CF (뮤지컬 취향 CF): {} (weight: {}%) → contribution: {}", 
                 musicalCfScore, (int)(MUSICAL_CF_WEIGHT * 100), musicalContribution);
        log.info("    - Popularity (인기차트): {} (weight: {}%) → contribution: {}", 
                 popularityScore, (int)(POPULARITY_WEIGHT * 100), popularityContribution);
        
        // 가장 높은 기여도 찾기
        double maxContribution = Math.max(Math.max(musicContribution, musicalContribution), popularityContribution);
        
        if (maxContribution > 0) {
            if (maxContribution == musicalContribution) {
                // 뮤지컬 취향 기반 추천 (50% - 가장 높은 가중치)
                // musicalCfScore는 이미 가중치가 적용된 값이므로, 원래 유사도는 musicalCfScore / MUSICAL_CF_WEIGHT
                // 하지만 이 값은 가중 평균 평점 (0~5)을 5로 나눈 값이므로 그대로 사용
                double similarityPercent = Math.min(musicalCfScore * 100, 100.0);
                item.setSimilarityPercentage(similarityPercent);
                item.setRecommendationReason(
                    String.format("뮤지컬 취향이 %.0f%% 유사한 사용자가 높게 평가한 작품입니다", 
                                similarityPercent)
                );
                log.info("  ✅ Reason: 뮤지컬 취향 CF ({}%)", (int)similarityPercent);
            } else if (maxContribution == musicContribution) {
                // 음악 취향 기반 추천 (30%)
                double similarityPercent = Math.min(musicCfScore * 100, 100.0);
                item.setSimilarityPercentage(similarityPercent);
                item.setRecommendationReason(
                    String.format("음악 취향이 %.0f%% 유사한 사용자가 높게 평가한 작품입니다", 
                                similarityPercent)
                );
                log.info("  ✅ Reason: 음악 취향 CF ({}%)", (int)similarityPercent);
            } else if (maxContribution == popularityContribution) {
                // 인기차트 기반 추천 (20%)
                if (item.getChartRanking() != null) {
                    item.setRecommendationReason(
                        String.format("인기차트 %d위 작품입니다", item.getChartRanking())
                    );
                    log.info("  ✅ Reason: 인기차트 {}위", item.getChartRanking());
                } else {
                    int ranking = (int) Math.ceil((1.0 - popularityScore) * 50) + 1;
                    item.setChartRanking(ranking);
                    item.setRecommendationReason(
                        String.format("인기차트 %d위 작품입니다", ranking)
                    );
                    log.info("  ✅ Reason: 인기차트 {}위 (calculated)", ranking);
                }
            }
        } else {
            // 모든 점수가 0인 경우
            item.setRecommendationReason("추천 작품입니다");
            log.info("  ⚠️ Reason: 기본 메시지 (모든 점수가 0)");
        }
        
        log.info("  📝 Final reason: '{}'", item.getRecommendationReason());
    }
    
    /**
     * 음악 취향만 기반 추천 이유 설정
     */
    private void setRecommendationReasonForMusicOnly(CandidateItem item, double avgRating) {
        // avgRating은 0~5 범위의 가중 평균 평점
        // 유사도 퍼센트로 변환: (평점 / 5) * 100
        double similarityPercent = Math.min((avgRating / 5.0) * 100, 100.0);
        item.setSimilarityPercentage(similarityPercent);
        item.setRecommendationReason(
            String.format("음악 취향이 %.0f%% 유사한 사용자가 높게 평가한 작품입니다", 
                        similarityPercent)
        );
        log.info("  ✅ Music-only reason: 음악 취향 CF ({}%)", (int)similarityPercent);
    }
    
    /**
     * 인기 차트만 기반 추천 이유 설정
     */
    private void setRecommendationReasonForPopularityOnly(CandidateItem item) {
        // chartRanking이 이미 설정되어 있으면 사용
        if (item.getChartRanking() != null) {
            item.setRecommendationReason(
                String.format("인기차트 %d위 작품입니다", item.getChartRanking())
            );
            log.info("  ✅ Popularity-only reason: 인기차트 {}위", item.getChartRanking());
        } else {
            // fallback: popularityScore로부터 순위 추정
            double popularityScore = item.getPopularityScore() != null ? item.getPopularityScore() : 0.0;
            int ranking = (int) Math.ceil((1.0 - popularityScore) * 50) + 1;
            item.setChartRanking(ranking);
            item.setRecommendationReason(
                String.format("인기차트 %d위 작품입니다", ranking)
            );
            log.info("  ✅ Popularity-only reason: 인기차트 {}위 (calculated)", ranking);
        }
    }




    /**
     * 신규 사용자 콜드스타트 처리
     */
    private List<CandidateItem> handleColdStartUser(User user, String region, int topM) {
        log.info("Handling cold start for user: {}", user.getId());

        // 인기 뮤지컬 기반 추천
        List<Musical> popularMusicals = musicalRepository.findAll().stream()
                .sorted((a, b) -> Double.compare(
                        b.getPopularityScore() != null ? b.getPopularityScore() : 0.0,
                        a.getPopularityScore() != null ? a.getPopularityScore() : 0.0))
                .limit(topM)
                .collect(Collectors.toList());

        return popularMusicals.stream()
                .map(musical -> CandidateItem.builder()
                        .musicalId(musical.getId())
                        .title(musical.getTitle())
                        .contentCrossScore(0.0)
                        .cfCrossScore(0.0)
                        .sideScore(musical.getPopularityScore() != null ? musical.getPopularityScore() : 0.0)
                        .stage1Score(musical.getPopularityScore() != null ? musical.getPopularityScore() : 0.0)
                        .genre(musical.getGenre())
                        .region(musical.getRegion())
                        .isOnSale(musical.getIsOnSale())
                        .isNew(musical.getIsNew())
                        .popularityScore(musical.getPopularityScore())
                        .source("popularity")
                        .build())
                .collect(Collectors.toList());
    }
}

