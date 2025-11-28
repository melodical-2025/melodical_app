package com.melodical.backend.service.recommendation;

import com.melodical.backend.dto.CandidateItem;
import com.melodical.backend.dto.RecommendationRequest;
import com.melodical.backend.entity.User;
import com.melodical.backend.entity.ExposureLog;
import com.melodical.backend.repository.ExposureLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Stage-3: Twiddler 후처리 서비스 (정책/다양성/신선도)
 * 랭커 결과를 사용자 경험과 비즈니스 규칙으로 미세조정
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Stage3TwiddlerService {

    private final ExposureLogRepository exposureLogRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final com.melodical.backend.repository.MusicalRepository musicalRepository;

    // 조정 가중치
    private static final double SEEN_PENALTY_WEIGHT = 0.3;
    private static final double FREQ_CAP_PENALTY = 0.5;
    private static final double SERIES_CLUSTER_PENALTY = 0.2;
    private static final double FRESHNESS_BOOST = 0.15;

    // 캐시 TTL
    private static final long SEEN_CACHE_TTL = 24; // 24시간
    private static final long FREQ_CACHE_TTL = 24; // 24시간

    /**
     * 후처리를 통한 최종 추천 목록 생성
     * ✨ 소스별 쿼터 보장: 뮤지컬 취향 7개, 음악 취향 5개, 인기차트 5개
     */
    public List<CandidateItem> applyTwiddlerPolicies(User user, List<CandidateItem> rankedCandidates,
                                                    RecommendationRequest request, Set<String> userRatedMusicals) {
        log.info("Applying Twiddler policies for user: {}, candidates: {}",
                user.getId(), rankedCandidates.size());

        // 1) 최근 본 아이템 패널티 적용
        applySeenPenalty(user, rankedCandidates);

        // 2) 일일 빈도 제한 적용
        applyFrequencyCapPenalty(user, rankedCandidates);

        // 3) 다양성 증진 (시리즈/제작사 클러스터링 방지)
        applyDiversityBoost(rankedCandidates);

        // 4) 신선도 가점 적용
        applyFreshnessBoost(rankedCandidates);

        // 5) 최종 점수 계산 및 정렬
        List<CandidateItem> finalCandidates = calculateFinalScores(rankedCandidates);

        // 6) 결정적 출력을 위한 안정적 정렬
        finalCandidates = applyStableSorting(finalCandidates);

        // ✨ 7) 소스별 쿼터 보장 (뮤지컬 취향 7개, 음악 취향 5개, 인기차트 5개)
        finalCandidates = applySourceQuotaGuarantee(finalCandidates, userRatedMusicals);

        // 8) 요청된 개수만큼 반환
        int requestedCount = request.getCount();
        List<CandidateItem> result = finalCandidates.stream()
                .limit(requestedCount)
                .collect(Collectors.toList());

        log.info("Applied Twiddler policies, returning {} candidates", result.size());
        log.info("  Source breakdown - Combined: {}, Music-only: {}, Popularity-only: {}",
                result.stream().filter(c -> "combined_algorithm".equals(c.getSource())).count(),
                result.stream().filter(c -> "music_taste_only".equals(c.getSource())).count(),
                result.stream().filter(c -> "popularity_only".equals(c.getSource())).count());
        
        return result;
    }

    /**
     * ✨ 소스별 쿼터 보장
     * - combined_algorithm: 최대 7개 보장
     * - music_taste_only: 최대 5개 보장
     * - popularity_only: 최대 5개 보장
     * 총 17개 후보가 있으면 모두 포함되도록 함
     * ✅ 사용자가 평가한 작품은 제외
     */
    private List<CandidateItem> applySourceQuotaGuarantee(List<CandidateItem> candidates, Set<String> userRatedMusicals) {
        // 소스별로 분류
        List<CandidateItem> combinedAlgorithm = candidates.stream()
                .filter(c -> "combined_algorithm".equals(c.getSource()))
                .collect(Collectors.toList());
        
        List<CandidateItem> musicTasteOnly = candidates.stream()
                .filter(c -> "music_taste_only".equals(c.getSource()))
                .collect(Collectors.toList());
        
        List<CandidateItem> popularityOnly = candidates.stream()
                .filter(c -> "popularity_only".equals(c.getSource()))
                .collect(Collectors.toList());
        
        log.info("📊 Source distribution before quota guarantee:");
        log.info("  - Combined algorithm: {} candidates", combinedAlgorithm.size());
        log.info("  - Music taste only: {} candidates", musicTasteOnly.size());
        log.info("  - Popularity only: {} candidates", popularityOnly.size());
        
        // 쿼터 보장을 위한 최종 리스트
        List<CandidateItem> result = new ArrayList<>();
        
        // 1) Combined algorithm 최대 7개
        result.addAll(combinedAlgorithm.stream().limit(7).collect(Collectors.toList()));
        
        // 2) Music taste only 최대 5개
        result.addAll(musicTasteOnly.stream().limit(5).collect(Collectors.toList()));
        
        // 3) Popularity only 최대 5개
        result.addAll(popularityOnly.stream().limit(5).collect(Collectors.toList()));

        // If popularity candidates are insufficient, supplement from musicalRepository
        int POPULARITY_REQUIRED = 5;
        long currentPopularityCount = result.stream().filter(c -> "popularity_only".equals(c.getSource())).count();
        if (currentPopularityCount < POPULARITY_REQUIRED) {
            int need = POPULARITY_REQUIRED - (int) currentPopularityCount;
            log.info("⚠️ Popularity quota not met. Need {} more candidates. Excluding {} user-rated musicals.",
                    need, userRatedMusicals.size());
            try {
                // Fetch top musicals by popularityScore and add missing ones
                List<com.melodical.backend.entity.Musical> topByPopularity = musicalRepository.findAll().stream()
                        .sorted((a, b) -> Double.compare(
                                b.getPopularityScore() != null ? b.getPopularityScore() : 0.0,
                                a.getPopularityScore() != null ? a.getPopularityScore() : 0.0))
                        .collect(Collectors.toList());

                for (com.melodical.backend.entity.Musical m : topByPopularity) {
                    if (need <= 0) break;
                    
                    // ✅ 사용자가 평가한 작품 제외
                    if (userRatedMusicals != null && m.getInterparkId() != null && 
                        userRatedMusicals.contains(m.getInterparkId())) {
                        log.debug("  Skipping user-rated musical: {} (ID: {}, Interpark: {})", 
                                m.getTitle(), m.getId(), m.getInterparkId());
                        continue;
                    }
                    
                    // 이미 결과에 포함된 작품 제외
                    boolean alreadyPresent = result.stream().anyMatch(c -> c.getMusicalId().equals(m.getId()));
                    if (alreadyPresent) continue;

                    CandidateItem candidate = CandidateItem.builder()
                            .musicalId(m.getId())
                            .title(m.getTitle())
                            .contentCrossScore(0.0)
                            .cfCrossScore(0.0)
                            .sideScore(m.getPopularityScore() != null ? m.getPopularityScore() : 0.0)
                            .stage1Score(m.getPopularityScore() != null ? m.getPopularityScore() : 0.0)
                            .genre(m.getGenre())
                            .region(m.getRegion())
                            .isOnSale(m.getIsOnSale())
                            .isNew(m.getIsNew())
                            .popularityScore(m.getPopularityScore())
                            .source("popularity_only")
                            .musicCfScore(0.0)
                            .musicalCfScore(0.0)
                            .pctrScore(m.getPopularityScore() != null ? m.getPopularityScore() : 0.0)
                            .finalScore(m.getPopularityScore() != null ? m.getPopularityScore() : 0.0)
                            .build();

                    result.add(candidate);
                    need--;
                    log.info("  ✅ Added supplemental popularity candidate: {} (ID: {})", 
                            m.getTitle(), m.getId());
                }
            } catch (Exception e) {
                log.warn("Failed to supplement popularity candidates from repository", e);
            }
        }
        
        log.info("✅ Source distribution after quota guarantee:");
        log.info("  - Combined algorithm: {} candidates", 
                result.stream().filter(c -> "combined_algorithm".equals(c.getSource())).count());
        log.info("  - Music taste only: {} candidates", 
                result.stream().filter(c -> "music_taste_only".equals(c.getSource())).count());
        log.info("  - Popularity only: {} candidates", 
                result.stream().filter(c -> "popularity_only".equals(c.getSource())).count());
        log.info("  - Total: {} candidates", result.size());
        
        // 점수 순으로 재정렬 (소스별 쿼터는 보장하되, 전체적으로는 점수순 유지)
        return result.stream()
                .sorted((a, b) -> {
                    int scoreCompare = Double.compare(b.getFinalScore(), a.getFinalScore());
                    if (scoreCompare == 0) {
                        return Long.compare(a.getMusicalId(), b.getMusicalId());
                    }
                    return scoreCompare;
                })
                .collect(Collectors.toList());
    }

    /**
     * 최근 본 아이템 패널티 적용 (시간감쇠)
     */
    private void applySeenPenalty(User user, List<CandidateItem> candidates) {
        String seenKey = "seen:" + user.getId();

        try {
            // Redis에서 최근 본 아이템들 조회 (ZSET: score=timestamp)
            Set<Object> seenItems = redisTemplate.opsForZSet()
                    .reverseRange(seenKey, 0, -1); // 최신순

            if (seenItems == null || seenItems.isEmpty()) {
                return;
            }

            // 현재 시간
            long currentTime = System.currentTimeMillis();

            for (CandidateItem candidate : candidates) {
                String itemKey = candidate.getMusicalId().toString();

                if (seenItems.contains(itemKey)) {
                    // 마지막 본 시간 조회
                    Double lastSeenScore = redisTemplate.opsForZSet().score(seenKey, itemKey);

                    if (lastSeenScore != null) {
                        long timeDiff = currentTime - lastSeenScore.longValue();
                        double hoursSince = timeDiff / (1000.0 * 60 * 60); // 시간 단위로 변환

                        // 시간감쇠: 최근 본 것일수록 큰 패널티
                        double penalty = SEEN_PENALTY_WEIGHT * Math.exp(-0.1 * hoursSince);
                        double currentScore = candidate.getPctrScore();
                        candidate.setPctrScore(currentScore * (1.0 - penalty));
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Failed to apply seen penalty for user: {}", user.getId(), e);
        }
    }

    /**
     * 일일 빈도 제한 적용
     */
    private void applyFrequencyCapPenalty(User user, List<CandidateItem> candidates) {
        String freqKey = "freq:" + user.getId() + ":" +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        try {
            for (CandidateItem candidate : candidates) {
                String itemKey = candidate.getMusicalId().toString();
                String fieldKey = freqKey + ":" + itemKey;

                // 오늘 이 아이템이 노출된 횟수 조회
                String countStr = (String) redisTemplate.opsForValue().get(fieldKey);
                int exposureCount = countStr != null ? Integer.parseInt(countStr) : 0;

                // 빈도 제한: 하루에 3번 이상 노출되면 패널티
                if (exposureCount >= 3) {
                    double penalty = FREQ_CAP_PENALTY * (exposureCount - 2) / 10.0;
                    double currentScore = candidate.getPctrScore();
                    candidate.setPctrScore(currentScore * (1.0 - Math.min(penalty, 0.8)));
                }
            }

        } catch (Exception e) {
            log.warn("Failed to apply frequency cap penalty for user: {}", user.getId(), e);
        }
    }

    /**
     * 다양성 증진 (연속된 유사 아이템 방지)
     */
    private void applyDiversityBoost(List<CandidateItem> candidates) {
        try {
            // 장르별 연속 방지
            String lastGenre = null;
            int consecutiveCount = 0;

            for (int i = 0; i < candidates.size(); i++) {
                CandidateItem candidate = candidates.get(i);
                String currentGenre = candidate.getGenre();

                if (currentGenre != null && currentGenre.equals(lastGenre)) {
                    consecutiveCount++;
                    // 3번째부터 패널티 적용
                    if (consecutiveCount >= 3) {
                        double penalty = SERIES_CLUSTER_PENALTY * (consecutiveCount - 2);
                        double currentScore = candidate.getPctrScore();
                        candidate.setPctrScore(currentScore * (1.0 - penalty));
                    }
                } else {
                    consecutiveCount = 1;
                    lastGenre = currentGenre;
                }
            }

        } catch (Exception e) {
            log.warn("Failed to apply diversity boost", e);
        }
    }

    /**
     * 신선도 가점 적용
     */
    private void applyFreshnessBoost(List<CandidateItem> candidates) {
        try {
            for (CandidateItem candidate : candidates) {
                // 신작 뮤지컬에 가점
                if (Boolean.TRUE.equals(candidate.getIsNew())) {
                    double currentScore = candidate.getPctrScore();
                    candidate.setPctrScore(currentScore * (1.0 + FRESHNESS_BOOST));
                }
            }

        } catch (Exception e) {
            log.warn("Failed to apply freshness boost", e);
        }
    }

    /**
     * 최종 점수 계산
     */
    private List<CandidateItem> calculateFinalScores(List<CandidateItem> candidates) {
        for (CandidateItem candidate : candidates) {
            // 최종 점수 = pCTR 점수 (이미 조정됨)
            candidate.setFinalScore(candidate.getPctrScore());
        }

        return candidates.stream()
                .sorted((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()))
                .collect(Collectors.toList());
    }

    /**
     * 안정적 정렬 (동일 점수 시 ID로 정렬)
     */
    private List<CandidateItem> applyStableSorting(List<CandidateItem> candidates) {
        return candidates.stream()
                .sorted((a, b) -> {
                    int scoreCompare = Double.compare(b.getFinalScore(), a.getFinalScore());
                    if (scoreCompare == 0) {
                        return Long.compare(a.getMusicalId(), b.getMusicalId());
                    }
                    return scoreCompare;
                })
                .collect(Collectors.toList());
    }

    /**
     * 본 아이템 캐시 업데이트
     */
    public void updateSeenItems(User user, List<CandidateItem> exposedItems) {
        String seenKey = "seen:" + user.getId();
        long currentTime = System.currentTimeMillis();

        try {
            for (CandidateItem item : exposedItems) {
                redisTemplate.opsForZSet().add(seenKey, item.getMusicalId().toString(), currentTime);
            }

            // TTL 설정
            redisTemplate.expire(seenKey, SEEN_CACHE_TTL, TimeUnit.HOURS);

        } catch (Exception e) {
            log.warn("Failed to update seen items for user: {}", user.getId(), e);
        }
    }

    /**
     * 빈도 카운터 업데이트
     */
    public void updateFrequencyCounters(User user, List<CandidateItem> exposedItems) {
        String dateKey = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        try {
            for (CandidateItem item : exposedItems) {
                String freqKey = "freq:" + user.getId() + ":" + dateKey + ":" + item.getMusicalId();
                redisTemplate.opsForValue().increment(freqKey);
                redisTemplate.expire(freqKey, FREQ_CACHE_TTL, TimeUnit.HOURS);
            }

        } catch (Exception e) {
            log.warn("Failed to update frequency counters for user: {}", user.getId(), e);
        }
    }
}
