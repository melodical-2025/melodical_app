package com.melodical.backend.service.recommendation;

import com.melodical.backend.dto.CandidateItem;
import com.melodical.backend.dto.RecommendationRequest;
import com.melodical.backend.entity.*;
import com.melodical.backend.repository.*;
import com.melodical.backend.service.model.PCTRModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Stage-2: pCTR 랭킹 서비스 (Precision 최적화)
 * 후보 각각의 클릭 확률을 예측하여 정밀한 순위 매김
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Stage2PCTRRankingService {

    private final UserMusicProfileRepository userMusicProfileRepository;
    private final MusicalFanProfileRepository musicalFanProfileRepository;
    private final ExposureLogRepository exposureLogRepository;
    private final InteractionLogRepository interactionLogRepository;
    private final PCTRModelService pctrModelService;

    /**
     * 후보들에 대한 pCTR 점수를 계산하여 랭킹
     */
    public List<CandidateItem> rankCandidatesByPCTR(User user, List<CandidateItem> candidates,
                                                   RecommendationRequest request) {
        log.info("Ranking {} candidates by pCTR for user: {}", candidates.size(), user.getId());

        // 각 후보에 대한 피처 생성 및 pCTR 점수 계산
        for (CandidateItem candidate : candidates) {
            Map<String, Double> features = extractFeatures(user, candidate, request);
            double pctrScore = predictPCTR(features);
            candidate.setPctrScore(pctrScore);
        }

        // pCTR 점수 기준으로 내림차순 정렬
        List<CandidateItem> rankedCandidates = candidates.stream()
                .sorted((a, b) -> Double.compare(b.getPctrScore(), a.getPctrScore()))
                .collect(Collectors.toList());

        log.info("Ranked {} candidates by pCTR", rankedCandidates.size());
        return rankedCandidates;
    }

    /**
     * 피처 추출 (사용자, 아이템, 교차, 컨텍스트)
     */
    private Map<String, Double> extractFeatures(User user, CandidateItem candidate,
                                              RecommendationRequest request) {
        Map<String, Double> features = new HashMap<>();

        // 1) 사용자 피처
        extractUserFeatures(user, features);

        // 2) 아이템 피처
        extractItemFeatures(candidate, features);

        // 3) 교차 피처
        extractCrossFeatures(user, candidate, features);

        // 4) 컨텍스트 피처
        extractContextFeatures(request, features);

        return features;
    }

    /**
     * 사용자 피처 추출
     */
    private void extractUserFeatures(User user, Map<String, Double> features) {
        // 사용자 음악 프로필 로드
        List<UserMusicProfile> profiles = userMusicProfileRepository.findByUser(user);

        // 장르별 선호도 (주요 장르들만)
        String[] mainGenres = {"Rock", "Pop", "Classical", "Jazz", "Electronic", "Hip-Hop", "R&B"};
        for (String genre : mainGenres) {
            double preference = profiles.stream()
                    .filter(p -> p.getGenreName().equals(genre))
                    .mapToDouble(UserMusicProfile::getPreferenceScore)
                    .findFirst()
                    .orElse(0.0);
            features.put("user_genre_" + genre.toLowerCase(), preference / 5.0); // 0-1 정규화
        }

        // 사용자 활동성 피처
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        List<InteractionLog> recentInteractions = interactionLogRepository
                .findByUserAndInteractionType(user, "click").stream()
                .filter(log -> log.getInteractionAt().isAfter(oneWeekAgo))
                .collect(Collectors.toList());

        features.put("user_recent_clicks", Math.min(recentInteractions.size() / 20.0, 1.0)); // 최대 20개로 정규화

        // 총 평점 개수 (프로필 강도)
        int totalRatings = profiles.stream()
                .mapToInt(UserMusicProfile::getRatingCount)
                .sum();
        features.put("user_profile_strength", Math.min(totalRatings / 50.0, 1.0)); // 최대 50개로 정규화

        // 사용자 지역 피처 (원핫 인코딩)
        // 여기서는 간단하게 서울 여부만 체크
        features.put("user_region_seoul", 1.0); // 실제로는 사용자 프로필에서 가져와야 함
    }

    /**
     * 아이템 피처 추출
     */
    private void extractItemFeatures(CandidateItem candidate, Map<String, Double> features) {
        // 판매중 여부
        features.put("item_is_on_sale", Boolean.TRUE.equals(candidate.getIsOnSale()) ? 1.0 : 0.0);

        // 신작 여부
        features.put("item_is_new", Boolean.TRUE.equals(candidate.getIsNew()) ? 1.0 : 0.0);

        // 인기도 점수
        features.put("item_popularity", candidate.getPopularityScore() != null ?
                     candidate.getPopularityScore() : 0.0);

        // 장르 원핫 인코딩
        String[] genres = {"로맨스", "코미디", "드라마", "액션", "판타지", "호러", "스릴러"};
        for (String genre : genres) {
            features.put("item_genre_" + genre,
                        genre.equals(candidate.getGenre()) ? 1.0 : 0.0);
        }

        // 지역 피처
        features.put("item_region_seoul", "서울".equals(candidate.getRegion()) ? 1.0 : 0.0);
        features.put("item_region_busan", "부산".equals(candidate.getRegion()) ? 1.0 : 0.0);

        // 가격대 피처 (임의 설정)
        features.put("item_price_range", 0.5); // 실제로는 가격 정보를 사용
    }

    /**
     * 교차 피처 추출
     */
    private void extractCrossFeatures(User user, CandidateItem candidate, Map<String, Double> features) {
        // Stage-1 점수들
        features.put("cross_content_score", candidate.getContentCrossScore());
        features.put("cross_cf_score", candidate.getCfCrossScore());
        features.put("cross_stage1_score", candidate.getStage1Score());

        // 콘텐츠 x CF 교차항
        features.put("cross_content_cf_interaction",
                     candidate.getContentCrossScore() * candidate.getCfCrossScore());

        // 과거 상호작용 여부 체크 - 수정된 버전
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        long pastExposureCount = exposureLogRepository.findAll().stream()
                .filter(log -> log.getUser().getId().equals(user.getId()) &&
                              log.getMusical().getId().equals(candidate.getMusicalId()) &&
                              log.getExposedAt() != null &&
                              log.getExposedAt().isAfter(oneMonthAgo))
                .count();

        features.put("cross_past_exposure", pastExposureCount > 0 ? 1.0 : 0.0);

        // 지역 일치 여부
        features.put("cross_region_match", "서울".equals(candidate.getRegion()) ? 1.0 : 0.0); // 사용자 지역과 비교
    }

    /**
     * 컨텍스트 피처 추출
     */
    private void extractContextFeatures(RecommendationRequest request, Map<String, Double> features) {
        // 시간대 피처
        int hour = LocalDateTime.now().getHour();
        features.put("context_hour_morning", (hour >= 6 && hour < 12) ? 1.0 : 0.0);
        features.put("context_hour_afternoon", (hour >= 12 && hour < 18) ? 1.0 : 0.0);
        features.put("context_hour_evening", (hour >= 18 && hour < 24) ? 1.0 : 0.0);
        features.put("context_hour_night", (hour >= 0 && hour < 6) ? 1.0 : 0.0);

        // 요일 피처
        int dayOfWeek = LocalDateTime.now().getDayOfWeek().getValue();
        features.put("context_is_weekend", (dayOfWeek == 6 || dayOfWeek == 7) ? 1.0 : 0.0);

        // Surface 피처
        features.put("context_surface_home", "home".equals(request.getSurface()) ? 1.0 : 0.0);
        features.put("context_surface_search", "search".equals(request.getSurface()) ? 1.0 : 0.0);
        features.put("context_surface_detail", "detail".equals(request.getSurface()) ? 1.0 : 0.0);

        // 디바이스 피처 (임의 설정)
        features.put("context_device_mobile", 1.0); // 실제로는 요청에서 파싱
    }

    /**
     * pCTR 예측 (PCTRModelService 사용)
     * 실제 운영에서는 LightGBM 등의 모델을 ONNX로 로딩하여 사용
     */
    private double predictPCTR(Map<String, Double> features) {
        return pctrModelService.predict(features);
    }

    /**
     * 추천 이유 생성 (설명가능성)
     */
    public List<String> generateReasons(User user, CandidateItem candidate, Map<String, Double> features) {
        List<String> reasons = new ArrayList<>();

        // 높은 점수를 가진 피처들 기반으로 이유 생성
        if (features.getOrDefault("cross_content_score", 0.0) > 0.7) {
            reasons.add("음악 취향과 잘 맞는 작품입니다");
        }

        if (features.getOrDefault("cross_cf_score", 0.0) > 0.6) {
            reasons.add("비슷한 취향의 사용자들이 좋아하는 작품입니다");
        }

        if (features.getOrDefault("item_is_on_sale", 0.0) > 0.0) {
            reasons.add("현재 예매 가능한 작품입니다");
        }

        if (features.getOrDefault("item_is_new", 0.0) > 0.0) {
            reasons.add("최신 작품입니다");
        }

        if (features.getOrDefault("item_popularity", 0.0) > 0.7) {
            reasons.add("인기 상승 중인 작품입니다");
        }

        if (features.getOrDefault("cross_region_match", 0.0) > 0.0) {
            reasons.add("가까운 지역에서 공연됩니다");
        }

        return reasons.isEmpty() ? List.of("추천 작품입니다") : reasons;
    }
}
