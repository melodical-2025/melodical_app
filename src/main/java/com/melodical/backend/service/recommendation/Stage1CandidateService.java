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

    // ✨ 새로운 가중치 설정 (음악 CF 30% + 뮤지컬 CF 50% + 인기차트 20%)
    private static final double MUSIC_CF_WEIGHT = 0.30;     // 음악 취향 기반 협업 필터링
    private static final double MUSICAL_CF_WEIGHT = 0.50;   // 뮤지컬 취향 기반 협업 필터링
    private static final double POPULARITY_WEIGHT = 0.20;   // 인기차트 기반

    /**
     * ✨ 새로운 추천 알고리즘: 평점 기반 협업 필터링
     * - 음악 취향 유사 사용자의 뮤지컬 추천 (30%)
     * - 뮤지컬 취향 유사 사용자의 뮤지컬 추천 (50%)
     * - 인기 차트 (20%)
     */
    public List<CandidateItem> generateCandidates(User user, String region, int topM) {
        log.info("🎯 Generating candidates for user: {} using rating-based CF", user.getId());

        // 사용자가 이미 평가한 뮤지컬 제외
        Set<String> userRatedMusicals = ratingBasedSimilarityService.getUserRatedMusicalIds(user.getId());
        log.info("  User has rated {} musicals", userRatedMusicals.size());

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

        // 6) 점수 통합 (30% + 50% + 20%)
        Map<String, CandidateItem> candidateMap = new HashMap<>();

        // 음악 기반 점수 추가 (30%)
        for (Map.Entry<String, Double> entry : musicBasedScores.entrySet()) {
            String musicalId = entry.getKey();
            double score = entry.getValue() * MUSIC_CF_WEIGHT;
            
            Optional<Musical> musicalOpt = findMusicalByInterparkId(musicalId);
            if (musicalOpt.isEmpty()) continue;
            
            Musical musical = musicalOpt.get();
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
                    .popularityScore(musical.getPopularityScore())
                    .source("music_cf")
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
                        .popularityScore(musical.getPopularityScore())
                        .source("musical_cf")
                        .musicCfScore(0.0)
                        .musicalCfScore(score)
                        .build();
                candidateMap.put(musicalId, candidate);
            }
        }

        // 7) 인기도 점수 추가 (20%)
        for (CandidateItem candidate : candidateMap.values()) {
            double popularityScore = popularityScores.getOrDefault(candidate.getMusicalId(), 0.0);
            double popularityContribution = popularityScore * POPULARITY_WEIGHT;
            candidate.setPopularityScore(popularityScore);
            candidate.setCfCrossScore(candidate.getCfCrossScore() + popularityContribution);
        }

        // 8) 최종 점수 계산 및 정렬
        List<CandidateItem> finalCandidates = candidateMap.values().stream()
                .peek(item -> {
                    // Stage1 점수 = CF 점수 (이미 가중치 적용됨)
                    item.setStage1Score(item.getCfCrossScore());
                    
                    // 추천 이유 설정
                    setRecommendationReason(item);
                })
                .sorted((a, b) -> Double.compare(b.getStage1Score(), a.getStage1Score()))
                .limit(topM)
                .collect(Collectors.toList());

        log.info("✅ Generated {} candidates (Music CF: {}, Musical CF: {}, Total: {})",
                finalCandidates.size(),
                (int) finalCandidates.stream().filter(c -> c.getMusicCfScore() > 0).count(),
                (int) finalCandidates.stream().filter(c -> c.getMusicalCfScore() > 0).count(),
                candidateMap.size());

        return finalCandidates;
    }

    /**
     * Interpark ID로 Musical 찾기
     */
    private Optional<Musical> findMusicalByInterparkId(String interparkId) {
        return musicalRepository.findAll().stream()
                .filter(m -> interparkId.equals(m.getInterparkId()))
                .findFirst();
    }

    /**
     * 인기도 점수 계산 (크롤링 데이터 기반)
     */
    private Map<Long, Double> getPopularityScores() {
        List<Musical> allMusicals = musicalRepository.findAll();
        Map<Long, Double> scores = new HashMap<>();
        
        // popularityScore 정규화 (0~1 범위)
        double maxScore = allMusicals.stream()
                .mapToDouble(m -> m.getPopularityScore() != null ? m.getPopularityScore() : 0.0)
                .max()
                .orElse(1.0);
        
        if (maxScore > 0) {
            for (Musical musical : allMusicals) {
                double normalized = (musical.getPopularityScore() != null ? musical.getPopularityScore() : 0.0) / maxScore;
                scores.put(musical.getId(), normalized);
            }
        }
        
        return scores;
    }

    /**
     * ✨ 새로운 추천 이유 설정 (평점 기반 CF)
     * 가장 높은 점수를 기준으로 추천 이유를 결정
     */
    private void setRecommendationReason(CandidateItem item) {
        double musicCfScore = item.getMusicCfScore() != null ? item.getMusicCfScore() : 0.0;
        double musicalCfScore = item.getMusicalCfScore() != null ? item.getMusicalCfScore() : 0.0;
        double popularityScore = item.getPopularityScore() != null ? item.getPopularityScore() : 0.0;
        
        // 가중치가 적용된 기여도 (이미 가중치가 적용된 상태)
        double musicContribution = musicCfScore;
        double musicalContribution = musicalCfScore;
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
                double similarityPercent = (musicalCfScore / MUSICAL_CF_WEIGHT) * 100;
                item.setSimilarityPercentage(Math.min(similarityPercent, 100.0));
                item.setRecommendationReason(
                    String.format("뮤지컬 취향이 %.0f%% 유사한 사용자가 높게 평가한 작품입니다", 
                                Math.min(similarityPercent, 100.0))
                );
                log.info("  ✅ Reason: 뮤지컬 취향 CF ({}%)", (int)Math.min(similarityPercent, 100.0));
            } else if (maxContribution == musicContribution) {
                // 음악 취향 기반 추천 (30%)
                double similarityPercent = (musicCfScore / MUSIC_CF_WEIGHT) * 100;
                item.setSimilarityPercentage(Math.min(similarityPercent, 100.0));
                item.setRecommendationReason(
                    String.format("음악 취향이 %.0f%% 유사한 사용자가 높게 평가한 작품입니다", 
                                Math.min(similarityPercent, 100.0))
                );
                log.info("  ✅ Reason: 음악 취향 CF ({}%)", (int)Math.min(similarityPercent, 100.0));
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

