package com.melodical.backend.service.recommendation;

import com.melodical.backend.entity.*;
import com.melodical.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 뮤지컬 팬 프로필 관리 서비스
 * A_m 벡터의 배치 업데이트 및 신뢰도 계산
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MusicalFanProfileService {

    private final MusicalFanProfileRepository musicalFanProfileRepository;
    private final UserMusicProfileRepository userMusicProfileRepository;
    private final InteractionLogRepository interactionLogRepository;
    private final MusicalRepository musicalRepository;

    // 최소 팬 수 (신뢰도 계산용)
    private static final int MIN_FANS_FOR_CONFIDENCE = 5;
    private static final double HIGH_RATING_THRESHOLD = 4.0;

    /**
     * 모든 뮤지컬의 팬 프로필 배치 업데이트 (15-60분 주기)
     * 
     * WARNING: This task is currently disabled due to performance issues.
     * It causes N+1 queries and can overload the database on startup.
     * Enable only when needed and after optimizing the query performance.
     */
    // @Scheduled(fixedRate = 900000, initialDelay = 120000) // DISABLED - causes performance issues
    @Transactional
    public void updateAllMusicalFanProfiles() {
        log.info("Starting batch update of musical fan profiles");

        try {
            List<Musical> musicals = musicalRepository.findAll();
            int updated = 0;
            
            // 배치 크기 제한 (한 번에 최대 50개만 처리)
            int batchSize = Math.min(musicals.size(), 50);
            
            for (int i = 0; i < batchSize; i++) {
                Musical musical = musicals.get(i);
                if (updateMusicalFanProfile(musical)) {
                    updated++;
                }
            }

            log.info("Updated fan profiles for {} out of {} musicals (batch size: {})", 
                    updated, musicals.size(), batchSize);

        } catch (Exception e) {
            log.error("Failed to update musical fan profiles", e);
        }
    }

    /**
     * 특정 뮤지컬의 팬 프로필 업데이트
     */
    @Transactional
    public boolean updateMusicalFanProfile(Musical musical) {
        try {
            // 해당 뮤지컬을 높게 평가한 사용자들 조회
            List<User> fans = interactionLogRepository.findUsersByMusicalRating(
                    musical, HIGH_RATING_THRESHOLD);

            if (fans.size() < MIN_FANS_FOR_CONFIDENCE) {
                log.debug("Not enough fans for musical {}: {} fans", musical.getId(), fans.size());
                return false;
            }

            // 팬들의 음악 프로필 수집
            Map<String, List<Double>> genreScores = new HashMap<>();

            for (User fan : fans) {
                List<UserMusicProfile> fanProfiles = userMusicProfileRepository.findByUser(fan);

                for (UserMusicProfile profile : fanProfiles) {
                    genreScores.computeIfAbsent(profile.getGenreName(), k -> new ArrayList<>())
                            .add(profile.getPreferenceScore());
                }
            }

            // 장르별 평균 계산 및 팬 프로필 업데이트
            for (Map.Entry<String, List<Double>> entry : genreScores.entrySet()) {
                String genreName = entry.getKey();
                List<Double> scores = entry.getValue();

                double averageScore = scores.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);

                double confidenceScore = calculateConfidenceScore(scores.size(), fans.size());

                // 기존 프로필 조회 또는 새로 생성
                MusicalFanProfile fanProfile = musicalFanProfileRepository
                        .findByMusicalAndGenreName(musical, genreName)
                        .orElse(MusicalFanProfile.builder()
                                .musical(musical)
                                .genreName(genreName)
                                .build());

                fanProfile.setFanPreferenceScore(averageScore);
                fanProfile.setFanCount(fans.size());
                fanProfile.setConfidenceScore(confidenceScore);

                musicalFanProfileRepository.save(fanProfile);
            }

            log.debug("Updated fan profile for musical {}: {} fans, {} genres",
                    musical.getId(), fans.size(), genreScores.size());

            return true;

        } catch (Exception e) {
            log.error("Failed to update fan profile for musical: {}", musical.getId(), e);
            return false;
        }
    }

    /**
     * 신작 뮤지컬의 초기 팬 프로필 생성
     */
    @Transactional
    public void createInitialFanProfile(Musical musical) {
        try {
            // 장르 태그 기반 초기 프로필 생성
            if (musical.getGenre() != null) {
                // 유사한 장르의 다른 뮤지컬들의 평균 팬 프로필 사용
                List<Musical> similarMusicals = musicalRepository.findAll().stream()
                        .filter(m -> musical.getGenre().equals(m.getGenre()) &&
                                    !m.getId().equals(musical.getId()))
                        .limit(5)
                        .collect(Collectors.toList());

                if (!similarMusicals.isEmpty()) {
                    Map<String, List<Double>> genreAverages = new HashMap<>();

                    for (Musical similar : similarMusicals) {
                        List<MusicalFanProfile> profiles = musicalFanProfileRepository
                                .findByMusical(similar);

                        for (MusicalFanProfile profile : profiles) {
                            genreAverages.computeIfAbsent(profile.getGenreName(), k -> new ArrayList<>())
                                    .add(profile.getFanPreferenceScore());
                        }
                    }

                    // 평균값으로 초기 프로필 생성
                    for (Map.Entry<String, List<Double>> entry : genreAverages.entrySet()) {
                        String genreName = entry.getKey();
                        double avgScore = entry.getValue().stream()
                                .mapToDouble(Double::doubleValue)
                                .average()
                                .orElse(0.0);

                        MusicalFanProfile initialProfile = MusicalFanProfile.builder()
                                .musical(musical)
                                .genreName(genreName)
                                .fanPreferenceScore(avgScore)
                                .fanCount(0)
                                .confidenceScore(0.1) // 낮은 초기 신뢰도
                                .build();

                        musicalFanProfileRepository.save(initialProfile);
                    }

                    log.info("Created initial fan profile for new musical: {}", musical.getId());
                }
            }

        } catch (Exception e) {
            log.error("Failed to create initial fan profile for musical: {}", musical.getId(), e);
        }
    }

    /**
     * 인기/신작 뮤지컬 우선 업데이트 (더 자주 갱신)
     */
    @Scheduled(fixedRate = 300000) // 5분마다 실행
    @Transactional
    public void updateHighPriorityMusicals() {
        try {
            // 인기 뮤지컬들 (상위 20개)
            List<Musical> popularMusicals = musicalRepository.findAll().stream()
                    .sorted((a, b) -> Double.compare(
                            b.getPopularityScore() != null ? b.getPopularityScore() : 0.0,
                            a.getPopularityScore() != null ? a.getPopularityScore() : 0.0))
                    .limit(20)
                    .collect(Collectors.toList());

            // 신작들
            List<Musical> newMusicals = musicalRepository.findAll().stream()
                    .filter(m -> Boolean.TRUE.equals(m.getIsNew()))
                    .collect(Collectors.toList());

            Set<Musical> priorityMusicals = new HashSet<>();
            priorityMusicals.addAll(popularMusicals);
            priorityMusicals.addAll(newMusicals);

            int updated = 0;
            for (Musical musical : priorityMusicals) {
                if (updateMusicalFanProfile(musical)) {
                    updated++;
                }
            }

            log.info("Updated {} high-priority musical fan profiles", updated);

        } catch (Exception e) {
            log.error("Failed to update high-priority musical fan profiles", e);
        }
    }

    /**
     * 뮤지컬 팬 프로필 유사도 계산
     */
    public double calculateMusicalSimilarity(Long musical1Id, Long musical2Id) {
        try {
            Musical musical1 = musicalRepository.findById(musical1Id).orElse(null);
            Musical musical2 = musicalRepository.findById(musical2Id).orElse(null);

            if (musical1 == null || musical2 == null) {
                return 0.0;
            }

            // 팬 프로필 조회
            List<MusicalFanProfile> profile1 = musicalFanProfileRepository.findByMusical(musical1);
            List<MusicalFanProfile> profile2 = musicalFanProfileRepository.findByMusical(musical2);

            if (profile1.isEmpty() || profile2.isEmpty()) {
                return 0.0;
            }

            // 장르 벡터로 변환
            Map<String, Double> vector1 = profile1.stream()
                    .collect(Collectors.toMap(
                            MusicalFanProfile::getGenreName,
                            MusicalFanProfile::getFanPreferenceScore // 올바른 메서드명 사용
                    ));

            Map<String, Double> vector2 = profile2.stream()
                    .collect(Collectors.toMap(
                            MusicalFanProfile::getGenreName,
                            MusicalFanProfile::getFanPreferenceScore // 올바른 메서드명 사용
                    ));

            // 코사인 유사도 계산
            return calculateCosineSimilarity(vector1, vector2);

        } catch (Exception e) {
            log.error("Failed to calculate musical similarity: {} vs {}", musical1Id, musical2Id, e);
            return 0.0;
        }
    }

    /**
     * 코사인 유사도 계산 헬퍼 메서드
     */
    private double calculateCosineSimilarity(Map<String, Double> vector1, Map<String, Double> vector2) {
        Set<String> commonKeys = new HashSet<>(vector1.keySet());
        commonKeys.retainAll(vector2.keySet());

        if (commonKeys.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (String key : commonKeys) {
            double val1 = vector1.getOrDefault(key, 0.0);
            double val2 = vector2.getOrDefault(key, 0.0);

            dotProduct += val1 * val2;
            norm1 += val1 * val1;
            norm2 += val2 * val2;
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 신뢰도 점수 계산
     */
    private double calculateConfidenceScore(int genreFanCount, int totalFanCount) {
        // 팬 수가 많을수록, 해당 장르를 좋아하는 팬 비율이 높을수록 신뢰도 증가
        double fanRatio = (double) genreFanCount / totalFanCount;
        double fanCountFactor = Math.min(totalFanCount / 50.0, 1.0); // 최대 50명 기준

        return fanRatio * fanCountFactor;
    }

    /**
     * 팬 프로필 품질 검증
     */
    public Map<String, Object> validateFanProfileQuality(Musical musical) {
        Map<String, Object> quality = new HashMap<>();

        try {
            List<MusicalFanProfile> profiles = musicalFanProfileRepository.findByMusical(musical);

            if (profiles.isEmpty()) {
                quality.put("status", "NO_PROFILE");
                return quality;
            }

            double avgConfidence = profiles.stream()
                    .mapToDouble(MusicalFanProfile::getConfidenceScore)
                    .average()
                    .orElse(0.0);

            int totalFans = profiles.stream()
                    .mapToInt(MusicalFanProfile::getFanCount)
                    .max()
                    .orElse(0);

            quality.put("status", "OK");
            quality.put("avg_confidence", avgConfidence);
            quality.put("total_fans", totalFans);
            quality.put("genre_count", profiles.size());
            quality.put("is_reliable", avgConfidence > 0.3 && totalFans >= MIN_FANS_FOR_CONFIDENCE);

        } catch (Exception e) {
            quality.put("status", "ERROR");
            quality.put("error", e.getMessage());
        }

        return quality;
    }
}
