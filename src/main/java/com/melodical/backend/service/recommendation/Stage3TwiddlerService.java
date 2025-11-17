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
     */
    public List<CandidateItem> applyTwiddlerPolicies(User user, List<CandidateItem> rankedCandidates,
                                                    RecommendationRequest request) {
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

        // 7) 요청된 개수만큼 반환
        int requestedCount = request.getCount();
        List<CandidateItem> result = finalCandidates.stream()
                .limit(requestedCount)
                .collect(Collectors.toList());

        log.info("Applied Twiddler policies, returning {} candidates", result.size());
        return result;
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
