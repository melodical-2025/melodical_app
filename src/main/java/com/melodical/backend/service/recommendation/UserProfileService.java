package com.melodical.backend.service.recommendation;

import com.melodical.backend.entity.*;
import com.melodical.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 사용자 음악 프로필 관리 서비스
 * U_music 벡터의 실시간 업데이트 및 시간감쇠 적용
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserMusicProfileRepository userMusicProfileRepository;
    private final InteractionLogRepository interactionLogRepository;
    private final UserRepository userRepository;

    // 시간감쇠 파라미터 (일 단위)
    private static final double TIME_DECAY_LAMBDA = 0.1;

    /**
     * 음악 평점 기반 사용자 프로필 즉시 업데이트
     */
    @Transactional
    public void updateUserProfileOnRating(Long userId, String genre, Double rating) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            // 기존 프로필 조회 또는 새로 생성
            UserMusicProfile profile = userMusicProfileRepository
                    .findByUserAndGenreName(user, genre)
                    .orElse(UserMusicProfile.builder()
                            .user(user)
                            .genreName(genre)
                            .preferenceScore(0.0)
                            .ratingCount(0)
                            .build());

            // 시간감쇠 적용된 가중평균 계산
            double currentScore = profile.getPreferenceScore();
            int currentCount = profile.getRatingCount();

            // 새로운 평점의 가중치 (최근일수록 높음)
            double timeWeight = calculateTimeWeight(LocalDateTime.now());

            // 가중평균 업데이트
            double totalWeight = currentCount + timeWeight;
            double newScore = (currentScore * currentCount + rating * timeWeight) / totalWeight;

            profile.setPreferenceScore(newScore);
            profile.setRatingCount(currentCount + 1);

            userMusicProfileRepository.save(profile);

            log.info("Updated user profile: userId={}, genre={}, newScore={}, count={}",
                    userId, genre, newScore, currentCount + 1);

        } catch (Exception e) {
            log.error("Failed to update user profile: userId={}, genre={}", userId, genre, e);
        }
    }

    /**
     * 배치 프로필 정규화 (주기적 실행)
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000) // 서버 시작 1분 후부터, 5분마다 실행
    @Transactional
    public void normalizeUserProfiles() {
        log.info("Starting batch user profile normalization");

        try {
            List<User> activeUsers = findActiveUsers();

            for (User user : activeUsers) {
                normalizeUserProfile(user);
            }

            log.info("Completed batch normalization for {} users", activeUsers.size());

        } catch (Exception e) {
            log.error("Failed to normalize user profiles", e);
        }
    }

    /**
     * 개별 사용자 프로필 정규화
     */
    @Transactional
    public void normalizeUserProfile(User user) {
        try {
            List<UserMusicProfile> profiles = userMusicProfileRepository.findByUser(user);

            if (profiles.isEmpty()) {
                return;
            }

            // 시간감쇠 적용
            LocalDateTime now = LocalDateTime.now();
            for (UserMusicProfile profile : profiles) {
                double daysSinceUpdate = calculateDaysSince(profile.getLastUpdated(), now);
                double decayFactor = Math.exp(-TIME_DECAY_LAMBDA * daysSinceUpdate);

                double decayedScore = profile.getPreferenceScore() * decayFactor;
                profile.setPreferenceScore(decayedScore);
            }

            // L2 정규화 적용
            double norm = Math.sqrt(profiles.stream()
                    .mapToDouble(p -> p.getPreferenceScore() * p.getPreferenceScore())
                    .sum());

            if (norm > 0) {
                for (UserMusicProfile profile : profiles) {
                    double normalizedScore = profile.getPreferenceScore() / norm;
                    profile.setPreferenceScore(normalizedScore);
                }

                userMusicProfileRepository.saveAll(profiles);
            }

            log.debug("Normalized profile for user: {}, norm: {}", user.getId(), norm);

        } catch (Exception e) {
            log.error("Failed to normalize profile for user: {}", user.getId(), e);
        }
    }

    /**
     * 초기 사용자 프로필 생성 (온보딩 시)
     */
    @Transactional
    public void createInitialProfile(Long userId, Map<String, Double> genreRatings) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            for (Map.Entry<String, Double> entry : genreRatings.entrySet()) {
                String genre = entry.getKey();
                Double rating = entry.getValue();

                UserMusicProfile profile = UserMusicProfile.builder()
                        .user(user)
                        .genreName(genre)
                        .preferenceScore(rating)
                        .ratingCount(1)
                        .lastUpdated(LocalDateTime.now())
                        .build();

                userMusicProfileRepository.save(profile);
            }

            log.info("Created initial profile for user: {} with {} genres",
                    userId, genreRatings.size());

        } catch (Exception e) {
            log.error("Failed to create initial profile for user: {}", userId, e);
        }
    }

    /**
     * 상호작용 기반 프로필 업데이트
     */
    @Transactional
    public void updateProfileFromInteraction(Long userId, Long musicalId, String interactionType) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            // 뮤지컬 정보 조회하여 장르 파악
            // 실제 구현에서는 MusicalRepository를 주입받아야 함
            log.info("Updating profile for user: {} based on {} interaction with musical: {}",
                    userId, interactionType, musicalId);

            // 암시적 피드백 처리 (찜하기, 클릭 등)
            double implicitRating = switch (interactionType) {
                case "wishlist" -> 4.0;
                case "click" -> 3.0;
                case "view" -> 2.0;
                default -> 1.0;
            };

            // 해당 장르의 선호도 업데이트 (간소화된 구현)
            // 실제로는 뮤지컬의 장르를 조회하여 해당 장르 프로필을 업데이트해야 함

        } catch (Exception e) {
            log.error("Failed to update profile from interaction", e);
        }
    }

    /**
     * 사용자 프로필 유사도 계산
     */
    public double calculateUserSimilarity(Long userId1, Long userId2) {
        try {
            User user1 = userRepository.findById(userId1).orElse(null);
            User user2 = userRepository.findById(userId2).orElse(null);

            if (user1 == null || user2 == null) {
                return 0.0;
            }

            List<UserMusicProfile> profile1 = userMusicProfileRepository.findByUser(user1);
            List<UserMusicProfile> profile2 = userMusicProfileRepository.findByUser(user2);

            Map<String, Double> vector1 = profile1.stream()
                    .collect(Collectors.toMap(
                            UserMusicProfile::getGenreName,
                            UserMusicProfile::getPreferenceScore
                    ));

            Map<String, Double> vector2 = profile2.stream()
                    .collect(Collectors.toMap(
                            UserMusicProfile::getGenreName,
                            UserMusicProfile::getPreferenceScore
                    ));

            return calculateCosineSimilarity(vector1, vector2);

        } catch (Exception e) {
            log.error("Failed to calculate user similarity: {} vs {}", userId1, userId2, e);
            return 0.0;
        }
    }

    /**
     * 활성 사용자 조회 (최근 30일 내 활동)
     */
    private List<User> findActiveUsers() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // InteractionLog에서 userId를 추출하여 User 엔티티 조회
        List<Long> activeUserIds = interactionLogRepository.findAll()
                .stream()
                .filter(log -> "rating".equals(log.getInteractionType()) && 
                              log.getInteractionAt() != null && 
                              log.getInteractionAt().isAfter(thirtyDaysAgo))
                .map(log -> log.getUser().getId())
                .distinct()
                .limit(1000) // 최대 1000명까지만 처리
                .collect(Collectors.toList());

        if (activeUserIds.isEmpty()) {
            return Collections.emptyList();
        }

        return userRepository.findAllById(activeUserIds);
    }

    /**
     * 시간 가중치 계산
     */
    private double calculateTimeWeight(LocalDateTime ratingTime) {
        // 최근 평점일수록 높은 가중치
        return 1.0; // 간단한 구현, 실제로는 시간 기반 계산
    }

    /**
     * 두 시점 간 일수 계산
     */
    private double calculateDaysSince(LocalDateTime from, LocalDateTime to) {
        if (from == null) {
            return 0.0;
        }
        return Math.abs(java.time.Duration.between(from, to).toDays());
    }

    /**
     * 뮤지컬 장르 조회
     */
    private String getMusicalGenre(Long musicalId) {
        // 실제로는 MusicalRepository에서 조회
        return "Pop"; // 임시값
    }

    /**
     * 상호작용 타입별 암시적 평점 계산
     */
    private double calculateImplicitRating(String interactionType) {
        return switch (interactionType) {
            case "click" -> 3.0;
            case "wishlist" -> 4.0;
            case "purchase" -> 5.0;
            default -> 0.0;
        };
    }

    /**
     * 코사인 유사도 계산
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
            double val1 = vector1.get(key);
            double val2 = vector2.get(key);

            dotProduct += val1 * val2;
            norm1 += val1 * val1;
            norm2 += val2 * val2;
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
