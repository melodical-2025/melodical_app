package com.melodical.backend.test;

import com.melodical.backend.dto.RecommendationRequest;
import com.melodical.backend.dto.RecommendationResponse;
import com.melodical.backend.entity.*;
import com.melodical.backend.repository.*;
import com.melodical.backend.service.recommendation.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 추천 시스템 테스트용 데이터 생성 및 테스트 실행 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationTestService {

    private final UserRepository userRepository;
    private final UserMusicProfileRepository userMusicProfileRepository;
    private final MusicalRepository musicalRepository;
    private final InteractionLogRepository interactionLogRepository;
    private final RecommendationService recommendationService;

    private final Random random = new Random();

    /**
     * 테스트 유저 생성 및 취향 프로필 설정
     */
    @Transactional
    public User createTestUserWithProfile(String username, String profileType) {
        log.info("Creating test user: {} with profile: {}", username, profileType);

        // 1. 테스트 유저 생성
        User testUser = User.builder()
                .name(username)
                .email(username + "@test.com")
                .password("test123")
                .provider("local")
                .role("USER")
                .build();
        testUser = userRepository.save(testUser);

        // 2. 프로필 타입에 따른 음악 취향 생성
        List<UserMusicProfile> profiles = createMusicProfileByType(testUser, profileType);
        userMusicProfileRepository.saveAll(profiles);

        log.info("Created test user: {} (ID: {}) with {} music profiles",
                username, testUser.getId(), profiles.size());

        return testUser;
    }

    /**
     * 프로필 타입별 음악 취향 생성
     */
    private List<UserMusicProfile> createMusicProfileByType(User user, String profileType) {
        List<UserMusicProfile> profiles = new ArrayList<>();

        switch (profileType.toLowerCase()) {
            case "rock_lover":
                profiles.add(createProfile(user, "Rock", 5.0, 50));
                profiles.add(createProfile(user, "Alternative", 4.5, 30));
                profiles.add(createProfile(user, "Indie", 4.0, 20));
                profiles.add(createProfile(user, "Pop", 3.0, 15));
                break;

            case "pop_fan":
                profiles.add(createProfile(user, "Pop", 5.0, 60));
                profiles.add(createProfile(user, "Dance", 4.5, 40));
                profiles.add(createProfile(user, "R&B", 4.0, 25));
                profiles.add(createProfile(user, "Electronic", 3.5, 20));
                break;

            case "classical_enthusiast":
                profiles.add(createProfile(user, "Classical", 5.0, 70));
                profiles.add(createProfile(user, "Jazz", 4.5, 45));
                profiles.add(createProfile(user, "Soundtrack", 4.0, 30));
                profiles.add(createProfile(user, "World", 3.5, 15));
                break;

            case "diverse":
                profiles.add(createProfile(user, "Pop", 4.0, 30));
                profiles.add(createProfile(user, "Rock", 4.0, 25));
                profiles.add(createProfile(user, "Jazz", 4.0, 20));
                profiles.add(createProfile(user, "Classical", 3.5, 20));
                profiles.add(createProfile(user, "R&B", 3.5, 15));
                profiles.add(createProfile(user, "Electronic", 3.0, 10));
                break;

            case "musical_fan":
                profiles.add(createProfile(user, "Soundtrack", 5.0, 80));
                profiles.add(createProfile(user, "Classical", 4.5, 50));
                profiles.add(createProfile(user, "Pop", 4.0, 40));
                profiles.add(createProfile(user, "Jazz", 3.5, 25));
                break;

            default:
                profiles.add(createProfile(user, "Pop", 4.0, 30));
                profiles.add(createProfile(user, "Rock", 3.5, 20));
                profiles.add(createProfile(user, "Classical", 3.0, 15));
        }

        return profiles;
    }

    private UserMusicProfile createProfile(User user, String genreName, double preference, int ratingCount) {
        return UserMusicProfile.builder()
                .user(user)
                .genreName(genreName)
                .preferenceScore(preference)
                .ratingCount(ratingCount)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    /**
     * 테스트 유저가 음악 평가 남기기 (시뮬레이션)
     */
    @Transactional
    public void simulateMusicRatings(User user, int count) {
        log.info("Simulating {} music ratings for user: {}", count, user.getId());

        List<UserMusicProfile> profiles = userMusicProfileRepository.findByUser(user);

        if (profiles.isEmpty()) {
            log.warn("No music profiles found for user: {}", user.getId());
            return;
        }

        List<String> preferredGenres = profiles.stream()
                .filter(p -> p.getPreferenceScore() >= 4.0)
                .map(UserMusicProfile::getGenreName)
                .toList();

        log.info("User's preferred genres: {}", preferredGenres);

        for (int i = 0; i < count; i++) {
            String genre = preferredGenres.isEmpty() ? "Pop" :
                    preferredGenres.get(random.nextInt(preferredGenres.size()));

            InteractionLog log = InteractionLog.builder()
                    .user(user)
                    .interactionType(i % 3 == 0 ? "rating" : "click")
                    .interactionAt(LocalDateTime.now().minusDays(random.nextInt(30)))
                    .ratingValue(i % 3 == 0 ? 4.0 + random.nextDouble() : null)
                    .build();

            interactionLogRepository.save(log);
        }

        log.info("Created {} interaction logs for user: {}", count, user.getId());
    }

    /**
     * 추천 테스트 실행
     */
    @Transactional(readOnly = true)
    public List<RecommendationResponse> testRecommendation(User user, String surface, int count) {
        log.info("Testing recommendation for user: {} on surface: {}", user.getId(), surface);

        RecommendationRequest request = new RecommendationRequest();
        request.setUserId(user.getId());
        request.setSurface(surface);
        request.setCount(count);
        request.setRegion("서울");

        List<RecommendationResponse> recommendations = recommendationService.recommend(request);

        log.info("Generated {} recommendations for user: {}", recommendations.size(), user.getId());

        return recommendations;
    }

    /**
     * 추천 결과 분석
     */
    public void analyzeRecommendations(User user, List<RecommendationResponse> recommendations) {
        log.info("\n========================================");
        log.info("추천 결과 분석 for User: {} ({})", user.getName(), user.getId());
        log.info("========================================");

        if (recommendations.isEmpty()) {
            log.warn("추천 결과가 없습니다.");
            return;
        }

        log.info("\n[추천된 뮤지컬 Top 10]");
        for (int i = 0; i < Math.min(10, recommendations.size()); i++) {
            RecommendationResponse rec = recommendations.get(i);
            Double score = rec.getFinalScore() != null ? rec.getFinalScore() : 0.0;
            log.info("{}. {} (점수: {:.3f}, 장르: {}, 지역: {})",
                    i + 1, rec.getTitle(), score, rec.getGenre(), rec.getRegion());
        }

        log.info("\n[장르 분포]");
        recommendations.stream()
                .map(RecommendationResponse::getGenre)
                .distinct()
                .forEach(genre -> {
                    long genreCount = recommendations.stream()
                            .filter(r -> genre != null && genre.equals(r.getGenre()))
                            .count();
                    log.info("{}: {} 개 ({}%)", genre, genreCount,
                            String.format("%.1f", genreCount * 100.0 / recommendations.size()));
                });

        double avgScore = recommendations.stream()
                .filter(r -> r.getFinalScore() != null)
                .mapToDouble(RecommendationResponse::getFinalScore)
                .average()
                .orElse(0.0);
        log.info("\n[평균 추천 점수]: {:.3f}", avgScore);

        long onSaleCount = recommendations.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsOnSale()))
                .count();
        long newCount = recommendations.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsNew()))
                .count();

        log.info("\n[판매 정보]");
        log.info("판매 중: {} 개 ({}%)", onSaleCount,
                String.format("%.1f", onSaleCount * 100.0 / recommendations.size()));
        log.info("신작: {} 개 ({}%)", newCount,
                String.format("%.1f", newCount * 100.0 / recommendations.size()));

        log.info("========================================\n");
    }

    /**
     * 통합 테스트 실행
     */
    @Transactional
    public void runFullTest(String username, String profileType) {
        log.info("\n╔════════════════════════════════════════╗");
        log.info("║   추천 시스템 통합 테스트 시작          ║");
        log.info("╚════════════════════════════════════════╝\n");

        try {
            log.info("Step 1: 테스트 유저 생성");
            User testUser = createTestUserWithProfile(username, profileType);

            log.info("\nStep 2: 음악 평가 시뮬레이션 (30개)");
            simulateMusicRatings(testUser, 30);

            log.info("\nStep 3: 추천 생성 (홈 화면, 20개)");
            List<RecommendationResponse> recommendations = testRecommendation(testUser, "home", 20);

            log.info("\nStep 4: 결과 분석");
            analyzeRecommendations(testUser, recommendations);

            log.info("\n✅ 테스트 완료!");

        } catch (Exception e) {
            log.error("테스트 실행 중 오류 발생", e);
            log.info("\n❌ 테스트 실패!");
        }
    }

    /**
     * 여러 프로필 타입 비교 테스트
     */
    @Transactional
    public void runComparisonTest() {
        log.info("\n╔════════════════════════════════════════╗");
        log.info("║   다양한 프로필 비교 테스트            ║");
        log.info("╚════════════════════════════════════════╝\n");

        String[] profileTypes = {"rock_lover", "pop_fan", "classical_enthusiast", "musical_fan", "diverse"};

        for (String profileType : profileTypes) {
            String username = "test_" + profileType + "_" + System.currentTimeMillis();

            try {
                log.info("\n" + "=".repeat(50));
                log.info("프로필 타입: {}", profileType.toUpperCase());
                log.info("=".repeat(50));

                User user = createTestUserWithProfile(username, profileType);
                simulateMusicRatings(user, 20);
                List<RecommendationResponse> recs = testRecommendation(user, "home", 10);
                analyzeRecommendations(user, recs);

                Thread.sleep(1000);

            } catch (Exception e) {
                log.error("프로필 타입 {} 테스트 실패", profileType, e);
            }
        }

        log.info("\n✅ 비교 테스트 완료!");
    }
}

