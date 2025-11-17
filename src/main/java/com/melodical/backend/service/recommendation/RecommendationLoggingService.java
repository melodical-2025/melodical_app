package com.melodical.backend.service.recommendation;

import com.melodical.backend.dto.CandidateItem;
import com.melodical.backend.dto.RecommendationRequest;
import com.melodical.backend.entity.*;
import com.melodical.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 추천 로깅 서비스 - Stage-4 구현
 * 노출, 클릭, 상호작용 로깅 및 폐루프 학습 데이터 수집
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationLoggingService {

    private final ExposureLogRepository exposureLogRepository;
    private final InteractionLogRepository interactionLogRepository;
    private final MusicalRepository musicalRepository;
    private final UserRepository userRepository;

    /**
     * 노출 로그 기록 (비동기)
     */
    @Async
    @Transactional
    public void logExposures(User user, List<CandidateItem> candidates,
                           RecommendationRequest request, String sessionId) {
        try {
            for (int i = 0; i < candidates.size(); i++) {
                CandidateItem candidate = candidates.get(i);

                Optional<Musical> musicalOpt = musicalRepository.findById(candidate.getMusicalId());
                if (musicalOpt.isPresent()) {
                    Musical musical = musicalOpt.get();

                    ExposureLog exposureLog = ExposureLog.builder()
                            .user(user)
                            .musical(musical)
                            .position(i + 1)
                            .surface(request.getSurface())
                            .stage1Score(candidate.getStage1Score())
                            .pctrScore(candidate.getPctrScore())
                            .finalScore(candidate.getFinalScore())
                            .sessionId(sessionId)
                            .build();

                    exposureLogRepository.save(exposureLog);
                }
            }

            log.info("Logged {} exposures for user: {}, session: {}",
                    candidates.size(), user.getId(), sessionId);

        } catch (Exception e) {
            log.error("Failed to log exposures for user: {}, session: {}",
                    user.getId(), sessionId, e);
        }
    }

    /**
     * 클릭 로그 기록
     */
    @Transactional
    public void logClick(Long userId, Long musicalId, String sessionId) {
        try {
            log.info("Logging click for user: {}, musical: {}, session: {}",
                    userId, musicalId, sessionId);

            // 실제 User 엔티티 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            Musical musical = musicalRepository.findById(musicalId).orElse(null);
            if (musical != null) {
                InteractionLog clickLog = InteractionLog.builder()
                        .user(user)
                        .musical(musical)
                        .interactionType("click")
                        .interactionAt(LocalDateTime.now()) // 올바른 필드명 사용
                        .sessionId(sessionId)
                        .build();

                interactionLogRepository.save(clickLog);
            }
        } catch (Exception e) {
            log.error("Failed to log click", e);
        }
    }

    /**
     * 평점 로그 기록
     */
    @Transactional
    public void logRating(Long userId, Long musicalId, Double rating, String sessionId) {
        try {
            log.info("Logging rating for user: {}, musical: {}, rating: {}, session: {}",
                    userId, musicalId, rating, sessionId);

            // 실제 User 엔티티 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            Musical musical = musicalRepository.findById(musicalId).orElse(null);
            if (musical != null) {
                InteractionLog ratingLog = InteractionLog.builder()
                        .user(user)
                        .musical(musical)
                        .interactionType("rating")
                        .ratingValue(rating)
                        .interactionAt(LocalDateTime.now()) // 올바른 필드명 사용
                        .sessionId(sessionId)
                        .build();

                interactionLogRepository.save(ratingLog);
            }
        } catch (Exception e) {
            log.error("Failed to log rating", e);
        }
    }

    /**
     * 찜하기 로그 기록
     */
    @Transactional
    public void logWishlist(Long userId, Long musicalId, String sessionId) {
        try {
            log.info("Logging wishlist for user: {}, musical: {}, session: {}",
                    userId, musicalId, sessionId);

            // 실제 User 엔티티 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            Musical musical = musicalRepository.findById(musicalId).orElse(null);
            if (musical != null) {
                InteractionLog wishlistLog = InteractionLog.builder()
                        .user(user)
                        .musical(musical)
                        .interactionType("wishlist")
                        .interactionAt(LocalDateTime.now()) // 올바른 필드명 사용
                        .sessionId(sessionId)
                        .build();

                interactionLogRepository.save(wishlistLog);
            }
        } catch (Exception e) {
            log.error("Failed to log wishlist", e);
        }
    }

    /**
     * CTR 계산 (성능 측정용)
     */
    public double calculateCTR(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // ExposureLog에서 해당 기간의 노출 수 조회
            List<ExposureLog> exposures = exposureLogRepository.findAll()
                    .stream()
                    .filter(log -> log.getExposedAt() != null &&
                                  log.getExposedAt().isAfter(startTime) &&
                                  log.getExposedAt().isBefore(endTime))
                    .toList();

            if (exposures.isEmpty()) {
                return 0.0;
            }

            // 해당 기간의 클릭 상호작용 수 조회
            long clickCount = interactionLogRepository.findAll()
                    .stream()
                    .filter(log -> "click".equals(log.getInteractionType()) &&
                                  log.getInteractionAt() != null &&
                                  log.getInteractionAt().isAfter(startTime) &&
                                  log.getInteractionAt().isBefore(endTime))
                    .count();

            double ctr = (double) clickCount / exposures.size();
            log.info("CTR calculation: {} clicks / {} exposures = {}",
                    clickCount, exposures.size(), ctr);

            return ctr;

        } catch (Exception e) {
            log.error("Failed to calculate CTR", e);
            return 0.0;
        }
    }

    /**
     * 성능 메트릭 수집
     */
    public Map<String, Object> collectPerformanceMetrics(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            Map<String, Object> metrics = new HashMap<>();

            // 기본 통계 - 직접 필터링으로 계산
            long totalExposures = exposureLogRepository.findAll()
                    .stream()
                    .filter(log -> log.getExposedAt() != null &&
                                  log.getExposedAt().isAfter(startTime) &&
                                  log.getExposedAt().isBefore(endTime))
                    .count();

            long totalInteractions = interactionLogRepository.findAll()
                    .stream()
                    .filter(log -> log.getInteractionAt() != null &&
                                  log.getInteractionAt().isAfter(startTime) &&
                                  log.getInteractionAt().isBefore(endTime))
                    .count();

            metrics.put("totalExposures", totalExposures);
            metrics.put("totalInteractions", totalInteractions);
            metrics.put("ctr", totalExposures > 0 ? (double) totalInteractions / totalExposures : 0.0);

            // 상호작용 타입별 통계 추가
            Map<String, Long> interactionTypeStats = interactionLogRepository.findAll()
                    .stream()
                    .filter(log -> log.getInteractionAt() != null &&
                                  log.getInteractionAt().isAfter(startTime) &&
                                  log.getInteractionAt().isBefore(endTime))
                    .collect(java.util.stream.Collectors.groupingBy(
                            InteractionLog::getInteractionType,
                            java.util.stream.Collectors.counting()
                    ));

            metrics.put("interactionTypeStats", interactionTypeStats);

            return metrics;
        } catch (Exception e) {
            log.error("Failed to collect performance metrics", e);
            return Map.of("error", e.getMessage());
        }
    }
}
