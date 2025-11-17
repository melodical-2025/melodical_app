package com.melodical.backend.controller;

import com.melodical.backend.entity.*;
import com.melodical.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 테스트용 데이터 생성 컨트롤러
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestDataController {

    private final UserRepository userRepository;
    private final MusicalRepository musicalRepository;
    private final InteractionLogRepository interactionLogRepository;
    private final UserMusicProfileRepository userMusicProfileRepository;

    /**
     * 테스트 데이터 생성
     */
    @PostMapping("/setup")
    public ResponseEntity<Map<String, Object>> setupTestData() {
        try {
            Map<String, Object> result = new HashMap<>();

            // 1. 테스트 사용자 생성
            User testUser = createTestUser();
            result.put("userId", testUser.getId());

            // 2. 테스트 뮤지컬들 생성
            int musicalCount = createTestMusicals();
            result.put("musicalCount", musicalCount);

            // 3. 사용자 프로필 생성
            createTestUserProfile(testUser);
            result.put("userProfileCreated", true);

            // 4. 상호작용 로그 생성
            int interactionCount = createTestInteractions(testUser);
            result.put("interactionCount", interactionCount);

            result.put("status", "success");
            result.put("message", "Test data created successfully");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to setup test data", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    private User createTestUser() {
        // 기존 테스트 유저가 있는지 확인
        return userRepository.findById(1L).orElseGet(() -> {
            User user = User.builder()
                    .email("test@melodical.com")
                    .name("Test User")
                    .provider("LOCAL")
                    .role("USER")
                    .build();
            return userRepository.save(user);
        });
    }

    private int createTestMusicals() {
        // 기존 뮤지컬이 있는지 확인
        if (musicalRepository.count() > 0) {
            return (int) musicalRepository.count();
        }

        String[] titles = {"레미제라블", "오페라의 유령", "라이온킹", "시카고", "맘마미아"};
        String[] genres = {"뮤지컬", "오페라", "가족뮤지컬", "재즈뮤지컬", "팝뮤지컬"};
        String[] theaters = {"충무아트센터", "샤롯데씨어터", "블루스퀘어", "디큐브아트센터", "세종문화회관"};

        for (int i = 0; i < titles.length; i++) {
            Musical musical = Musical.builder()
                    .title(titles[i])
                    .genre(genres[i])
                    .theater(theaters[i])
                    .startDate(LocalDate.now().plusDays(30).toString())
                    .endDate(LocalDate.now().plusDays(120).toString())
                    .region("서울")
                    .priceMin(50000)
                    .priceMax(150000)
                    .isOnSale(true)
                    .isNew(i < 2) // 처음 2개는 신작으로 설정
                    .popularityScore(0.8 - (i * 0.1))
                    .tags(genres[i] + ",클래식,인기")
                    .posterUrl("https://example.com/poster" + (i+1) + ".jpg")
                    .build();

            musicalRepository.save(musical);
        }

        return titles.length;
    }

    private void createTestUserProfile(User user) {
        // 기존 프로필이 있는지 확인
        if (userMusicProfileRepository.findByUser(user).isEmpty()) {
            String[] genres = {"뮤지컬", "오페라", "가족뮤지컬", "재즈뮤지컬", "팝뮤지컬"};
            double[] scores = {0.9, 0.7, 0.6, 0.8, 0.75};

            for (int i = 0; i < genres.length; i++) {
                UserMusicProfile profile = UserMusicProfile.builder()
                        .user(user)
                        .genreName(genres[i])
                        .preferenceScore(scores[i])
                        .build();

                userMusicProfileRepository.save(profile);
            }
        }
    }

    private int createTestInteractions(User user) {
        // 기존 상호작용이 있는지 확인
        if (!interactionLogRepository.findByUserAndInteractionType(user, "rating").isEmpty()) {
            return interactionLogRepository.findByUserAndInteractionType(user, "rating").size();
        }

        // 뮤지컬들에 대한 평점 생성
        return musicalRepository.findAll().stream()
                .mapToInt(musical -> {
                    // 클릭 로그
                    InteractionLog clickLog = InteractionLog.builder()
                            .user(user)
                            .musical(musical)
                            .interactionType("click")
                            .interactionAt(LocalDateTime.now().minusDays(1))
                            .sessionId("test_session_1")
                            .build();
                    interactionLogRepository.save(clickLog);

                    // 평점 로그 (1~5 사이 랜덤)
                    double rating = 3.0 + (Math.random() * 2.0); // 3.0~5.0 사이
                    InteractionLog ratingLog = InteractionLog.builder()
                            .user(user)
                            .musical(musical)
                            .interactionType("rating")
                            .ratingValue(rating)
                            .interactionAt(LocalDateTime.now().minusHours(12))
                            .sessionId("test_session_1")
                            .build();
                    interactionLogRepository.save(ratingLog);

                    return 2; // 클릭 + 평점 = 2개
                })
                .sum();
    }
}
