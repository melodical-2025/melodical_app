package com.melodical.backend.controller;

import com.melodical.backend.dto.RecommendationResponse;
import com.melodical.backend.entity.User;
import com.melodical.backend.test.RecommendationTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 추천 시스템 테스트 API
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class RecommendationTestController {

    private final RecommendationTestService testService;

    /**
     * 테스트 유저 생성 및 추천 테스트 실행
     */
    @PostMapping("/recommendation/full")
    public ResponseEntity<Map<String, Object>> runFullTest(
            @RequestParam(defaultValue = "test_user") String username,
            @RequestParam(defaultValue = "musical_fan") String profileType) {

        log.info("Starting full recommendation test: username={}, profileType={}",
                username, profileType);

        try {
            testService.runFullTest(username, profileType);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Test completed successfully");
            response.put("username", username);
            response.put("profileType", profileType);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Test execution failed", e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 테스트 유저만 생성
     */
    @PostMapping("/user/create")
    public ResponseEntity<Map<String, Object>> createTestUser(
            @RequestParam String username,
            @RequestParam(defaultValue = "musical_fan") String profileType) {

        log.info("Creating test user: username={}, profileType={}", username, profileType);

        try {
            User user = testService.createTestUserWithProfile(username, profileType);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("userId", user.getId());
            response.put("username", user.getName());
            response.put("email", user.getEmail());
            response.put("profileType", profileType);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("User creation failed", e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 음악 평가 시뮬레이션
     */
    @PostMapping("/user/{userId}/simulate-ratings")
    public ResponseEntity<Map<String, Object>> simulateRatings(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "30") int count) {

        log.info("Simulating ratings for user: userId={}, count={}", userId, count);

        try {
            // 유저 조회는 testService 내부에서 처리
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", String.format("Simulated %d ratings", count));
            response.put("userId", userId);
            response.put("count", count);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Rating simulation failed", e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 여러 프로필 타입 비교 테스트
     */
    @PostMapping("/recommendation/compare")
    public ResponseEntity<Map<String, Object>> runComparisonTest() {
        log.info("Starting comparison test for multiple profile types");

        try {
            testService.runComparisonTest();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Comparison test completed");
            response.put("profileTypes", List.of("rock_lover", "pop_fan", "classical_enthusiast",
                                                 "musical_fan", "diverse"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Comparison test failed", e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 사용 가능한 프로필 타입 목록
     */
    @GetMapping("/profile-types")
    public ResponseEntity<Map<String, Object>> getProfileTypes() {
        Map<String, Object> response = new HashMap<>();

        Map<String, String> profileTypes = new HashMap<>();
        profileTypes.put("rock_lover", "록 애호가 - Rock, Alternative, Indie 선호");
        profileTypes.put("pop_fan", "팝 팬 - Pop, Dance, R&B 선호");
        profileTypes.put("classical_enthusiast", "클래식 애호가 - Classical, Jazz, Soundtrack 선호");
        profileTypes.put("musical_fan", "뮤지컬 팬 - Soundtrack, Classical, Pop 선호");
        profileTypes.put("diverse", "다양한 취향 - 여러 장르 골고루 선호");

        response.put("status", "success");
        response.put("profileTypes", profileTypes);

        return ResponseEntity.ok(response);
    }

    /**
     * 테스트 가이드
     */
    @GetMapping("/guide")
    public ResponseEntity<Map<String, Object>> getTestGuide() {
        Map<String, Object> response = new HashMap<>();

        Map<String, String> guide = new HashMap<>();
        guide.put("step1", "GET /api/test/profile-types - 사용 가능한 프로필 타입 확인");
        guide.put("step2", "POST /api/test/recommendation/full?username=test1&profileType=musical_fan - 전체 테스트 실행");
        guide.put("step3", "서버 로그에서 상세 결과 확인");
        guide.put("alternative", "POST /api/test/recommendation/compare - 여러 프로필 타입 비교");

        response.put("status", "success");
        response.put("guide", guide);
        response.put("example", "curl -X POST 'http://localhost:8080/api/test/recommendation/full?username=test1&profileType=musical_fan'");

        return ResponseEntity.ok(response);
    }
}

