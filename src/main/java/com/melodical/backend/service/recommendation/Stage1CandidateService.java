package com.melodical.backend.service.recommendation;

import com.melodical.backend.dto.CandidateItem;
import com.melodical.backend.dto.UserMusicProfileDto;
import com.melodical.backend.entity.*;
import com.melodical.backend.repository.*;
import com.melodical.backend.service.crawler.CrawledDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Stage-1: 후보 생성 서비스 (Recall 최적화)
 * 콘텐츠 기반 + 협업 필터링 + 크롤링 데이터로 Top-M 후보를 빠르게 수집
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Stage1CandidateService {

    private final UserMusicProfileRepository userMusicProfileRepository;
    private final MusicalFanProfileRepository musicalFanProfileRepository;
    private final MusicalRepository musicalRepository;
    private final InteractionLogRepository interactionLogRepository;
    private final UserRepository userRepository;
    private final CrawledDataService crawledDataService;

    // 가중치 설정
    private static final double CONTENT_WEIGHT = 0.50;
    private static final double CF_WEIGHT = 0.35;
    private static final double CRAWLED_WEIGHT = 0.10;
    private static final double SIDE_WEIGHT = 0.05;

    /**
     * 사용자에 대한 후보 아이템들을 생성
     */
    public List<CandidateItem> generateCandidates(User user, String region, int topM) {
        log.info("Generating candidates for user: {}, topM: {}", user.getId(), topM);

        // 사용자 음악 프로필 로드
        UserMusicProfileDto userProfile = loadUserMusicProfile(user);
        if (userProfile.getGenrePreferences().isEmpty()) {
            return handleColdStartUser(user, region, topM);
        }

        // 1) 콘텐츠 기반 후보 생성
        List<CandidateItem> contentCandidates = generateContentBasedCandidates(userProfile, topM / 2);

        // 2) 협업 필터링 후보 생성
        List<CandidateItem> cfCandidates = generateCollaborativeFilteringCandidates(user, topM / 2);

        // 3) 크롤링 데이터 기반 인기 후보 추가
        List<CandidateItem> crawledCandidates = generateCrawledDataCandidates(topM / 4);

        // 4) 후보들을 통합하고 중복 제거
        Map<Long, CandidateItem> candidateMap = new HashMap<>();

        // 콘텐츠 기반 후보 추가
        for (CandidateItem item : contentCandidates) {
            candidateMap.put(item.getMusicalId(), item);
        }

        // 협업 필터링 후보 추가 (기존 아이템이 있으면 점수 통합)
        for (CandidateItem item : cfCandidates) {
            Long musicalId = item.getMusicalId();
            if (candidateMap.containsKey(musicalId)) {
                CandidateItem existing = candidateMap.get(musicalId);
                existing.setCfCrossScore(item.getCfCrossScore());
            } else {
                candidateMap.put(musicalId, item);
            }
        }

        // 크롤링 데이터 후보 추가 (인기도 부스트)
        for (CandidateItem item : crawledCandidates) {
            Long musicalId = item.getMusicalId();
            if (candidateMap.containsKey(musicalId)) {
                CandidateItem existing = candidateMap.get(musicalId);
                // 크롤링 데이터가 있으면 인기도 점수 부스트
                existing.setPopularityScore(Math.max(existing.getPopularityScore(), item.getPopularityScore()));
            } else {
                candidateMap.put(musicalId, item);
            }
        }

        // 5) 보조 점수 계산 및 최종 점수 산출
        List<CandidateItem> finalCandidates = candidateMap.values().stream()
                .map(item -> calculateFinalScore(item, region))
                .sorted((a, b) -> Double.compare(b.getStage1Score(), a.getStage1Score()))
                .limit(topM)
                .collect(Collectors.toList());

        log.info("Generated {} candidates for user: {} (content: {}, cf: {}, crawled: {})",
                finalCandidates.size(), user.getId(),
                contentCandidates.size(), cfCandidates.size(), crawledCandidates.size());
        return finalCandidates;
    }

    /**
     * 콘텐츠 기반 후보 생성 (content_cross)
     */
    private List<CandidateItem> generateContentBasedCandidates(UserMusicProfileDto userProfile, int topK) {
        List<CandidateItem> candidates = new ArrayList<>();

        // 모든 뮤지컬의 팬 프로필과 사용자 프로필 간 코사인 유사도 계산
        List<Musical> allMusicals = musicalRepository.findAll();

        for (Musical musical : allMusicals) {
            List<MusicalFanProfile> fanProfiles = musicalFanProfileRepository.findByMusical(musical);

            if (!fanProfiles.isEmpty()) {
                double contentScore = calculateContentCrossScore(userProfile, fanProfiles);

                CandidateItem candidate = CandidateItem.builder()
                        .musicalId(musical.getId())
                        .title(musical.getTitle())
                        .contentCrossScore(contentScore)
                        .cfCrossScore(0.0)
                        .sideScore(0.0)
                        .genre(musical.getGenre())
                        .region(musical.getRegion())
                        .isOnSale(musical.getIsOnSale())
                        .isNew(musical.getIsNew())
                        .popularityScore(musical.getPopularityScore())
                        .source("content")
                        .build();

                candidates.add(candidate);
            }
        }

        return candidates.stream()
                .sorted((a, b) -> Double.compare(b.getContentCrossScore(), a.getContentCrossScore()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 협업 필터링 후보 생성 (cf_cross)
     */
    private List<CandidateItem> generateCollaborativeFilteringCandidates(User user, int topK) {
        List<CandidateItem> candidates = new ArrayList<>();

        // 1) 유사한 사용자들 찾기 (음악 취향 기반)
        List<User> similarUsers = findSimilarUsers(user, 50); // Top-50 유사 사용자

        if (similarUsers.isEmpty()) {
            return candidates;
        }

        // 2) 유사 사용자들이 높게 평가한 뮤지컬들 수집
        List<InteractionLog> highRatings = interactionLogRepository.findHighRatingsByUser(user, 4.0);
        Set<Long> userRatedMusicals = highRatings.stream()
                .map(log -> log.getMusical().getId())
                .collect(Collectors.toSet());

        Map<Long, Double> musicalScores = new HashMap<>();
        Map<Long, String> musicalTitles = new HashMap<>();

        for (User similarUser : similarUsers) {
            List<InteractionLog> similarUserRatings = interactionLogRepository
                    .findHighRatingsByUser(similarUser, 4.0);

            double userSimilarity = calculateUserSimilarity(user, similarUser);

            for (InteractionLog rating : similarUserRatings) {
                Long musicalId = rating.getMusical().getId();

                // 이미 평가한 뮤지컬은 제외
                if (!userRatedMusicals.contains(musicalId)) {
                    double normalizedRating = (rating.getRatingValue() - 2.5) / 2.5; // [-1, 1] 정규화
                    double weightedScore = userSimilarity * normalizedRating;

                    musicalScores.merge(musicalId, weightedScore, Double::sum);
                    musicalTitles.putIfAbsent(musicalId, rating.getMusical().getTitle());
                }
            }
        }

        // 3) 점수 기준으로 정렬하여 후보 생성
        candidates = musicalScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> CandidateItem.builder()
                        .musicalId(entry.getKey())
                        .title(musicalTitles.get(entry.getKey()))
                        .contentCrossScore(0.0)
                        .cfCrossScore(entry.getValue())
                        .sideScore(0.0)
                        .source("cf")
                        .build())
                .collect(Collectors.toList());

        return candidates;
    }

    /**
     * 콘텐츠 기반 점수 계산 (코사인 유사도)
     */
    private double calculateContentCrossScore(UserMusicProfileDto userProfile,
                                            List<MusicalFanProfile> fanProfiles) {
        Map<String, Double> userGenres = userProfile.getNormalizedPreferences();
        Map<String, Double> fanGenres = new HashMap<>();

        // 팬 프로필을 장르별로 집계
        for (MusicalFanProfile fanProfile : fanProfiles) {
            fanGenres.put(fanProfile.getGenreName(), fanProfile.getFanPreferenceScore());
        }

        // 팬 프로필 정규화
        fanGenres = normalizeVector(fanGenres);

        // 코사인 유사도 계산
        return calculateCosineSimilarity(userGenres, fanGenres);
    }

    /**
     * 사용자 간 유사도 계산
     */
    private double calculateUserSimilarity(User user1, User user2) {
        UserMusicProfileDto profile1 = loadUserMusicProfile(user1);
        UserMusicProfileDto profile2 = loadUserMusicProfile(user2);

        return calculateCosineSimilarity(
                profile1.getNormalizedPreferences(),
                profile2.getNormalizedPreferences()
        );
    }

    /**
     * 유사한 사용자들 찾기
     */
    private List<User> findSimilarUsers(User user, int topK) {
        // 실제 구현에서는 더 효율적인 방법 (예: LSH, 근사 최근접 이웃) 사용 가능
        // 여기서는 간단한 구현
        UserMusicProfileDto userProfile = loadUserMusicProfile(user);
        List<User> allUsers = userRepository.findAll();

        return allUsers.stream()
                .filter(u -> !u.getId().equals(user.getId()))
                .map(u -> new AbstractMap.SimpleEntry<>(u, calculateUserSimilarity(user, u)))
                .filter(entry -> entry.getValue() > 0.1) // 최소 유사도 임계값
                .sorted(Map.Entry.<User, Double>comparingByValue().reversed())
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 최종 점수 계산 (Stage-1)
     */
    private CandidateItem calculateFinalScore(CandidateItem item, String region) {
        double sideScore = calculateSideScore(item, region);
        item.setSideScore(sideScore);

        double finalScore = CONTENT_WEIGHT * item.getContentCrossScore() +
                           CF_WEIGHT * item.getCfCrossScore() +
                           SIDE_WEIGHT * sideScore;

        item.setStage1Score(finalScore);
        return item;
    }

    /**
     * 보조 점수 계산 (지역, 판매중, 신작 등)
     */
    private double calculateSideScore(CandidateItem item, String region) {
        double score = 0.0;

        // 지역 일치 가점
        if (region != null && region.equals(item.getRegion())) {
            score += 0.3;
        }

        // 판매중 가점
        if (Boolean.TRUE.equals(item.getIsOnSale())) {
            score += 0.4;
        }

        // 신작 가점
        if (Boolean.TRUE.equals(item.getIsNew())) {
            score += 0.2;
        }

        // 인기도 가점 (0-1 정규화)
        if (item.getPopularityScore() != null) {
            score += item.getPopularityScore() * 0.1;
        }

        return Math.min(score, 1.0); // 최대 1.0으로 제한
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

    /**
     * 벡터 정규화
     */
    private Map<String, Double> normalizeVector(Map<String, Double> vector) {
        double norm = Math.sqrt(vector.values().stream()
                .mapToDouble(v -> v * v)
                .sum());

        if (norm == 0.0) {
            return vector;
        }

        return vector.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() / norm
                ));
    }

    /**
     * 사용자 음악 프로필 로드
     */
    private UserMusicProfileDto loadUserMusicProfile(User user) {
        List<UserMusicProfile> profiles = userMusicProfileRepository.findByUser(user);

        Map<String, Double> genrePreferences = profiles.stream()
                .collect(Collectors.toMap(
                        UserMusicProfile::getGenreName,
                        UserMusicProfile::getPreferenceScore
                ));

        Map<String, Double> normalizedPreferences = normalizeVector(genrePreferences);

        int totalRatings = profiles.stream()
                .mapToInt(UserMusicProfile::getRatingCount)
                .sum();

        return UserMusicProfileDto.builder()
                .userId(user.getId())
                .genrePreferences(genrePreferences)
                .normalizedPreferences(normalizedPreferences)
                .totalRatings(totalRatings)
                .profileStrength(Math.min(totalRatings / 50.0, 1.0)) // 50개 평점에서 최대 강도
                .build();
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

    /**
     * 크롤링 데이터 기반 인기 후보 생성
     * integrated_weekly_dataset과 integrated_monthly_dataset 활용
     */
    private List<CandidateItem> generateCrawledDataCandidates(int topK) {
        List<CandidateItem> candidates = new ArrayList<>();

        try {
            // 주간 인기 뮤지컬 (최신 트렌드 반영)
            List<CrawledMusicalRanking> weeklyPopular =
                    crawledDataService.getTopWeeklyMusicals(topK / 2);

            // 월간 인기 뮤지컬 (안정적인 인기작)
            List<CrawledMusicalRanking> monthlyPopular =
                    crawledDataService.getTopMonthlyMusicals(topK / 2);

            // 주간 인기 뮤지컬 후보 생성
            for (CrawledMusicalRanking ranking : weeklyPopular) {
                CandidateItem candidate = createCandidateFromCrawledData(ranking, "weekly");
                if (candidate != null) {
                    candidates.add(candidate);
                }
            }

            // 월간 인기 뮤지컬 후보 생성
            for (CrawledMusicalRanking ranking : monthlyPopular) {
                CandidateItem candidate = createCandidateFromCrawledData(ranking, "monthly");
                if (candidate != null) {
                    candidates.add(candidate);
                }
            }

            log.debug("Generated {} crawled data candidates", candidates.size());

        } catch (Exception e) {
            log.error("Error generating crawled data candidates", e);
        }

        return candidates;
    }

    /**
     * CrawledMusicalRanking을 CandidateItem으로 변환
     */
    private CandidateItem createCandidateFromCrawledData(CrawledMusicalRanking ranking, String source) {
        try {
            // KOPIS Musical과 매칭 시도
            Optional<Musical> matchedMusical = findMatchingKopisMusical(ranking);

            if (matchedMusical.isEmpty()) {
                log.debug("No matching KOPIS musical found for: {}", ranking.getTitle());
                return null;
            }

            Musical musical = matchedMusical.get();

            // 크롤링 데이터 기반 인기도 점수 계산
            double popularityScore = crawledDataService.calculatePopularityScore(ranking);

            return CandidateItem.builder()
                    .musicalId(musical.getId())
                    .title(musical.getTitle())
                    .contentCrossScore(0.0)
                    .cfCrossScore(0.0)
                    .sideScore(popularityScore * CRAWLED_WEIGHT)
                    .genre(musical.getGenre())
                    .region(musical.getRegion())
                    .isOnSale(musical.getIsOnSale())
                    .isNew(musical.getIsNew())
                    .popularityScore(popularityScore)
                    .source("crawled_" + source)
                    .build();

        } catch (Exception e) {
            log.error("Error creating candidate from crawled data", e);
            return null;
        }
    }

    /**
     * 크롤링 데이터와 매칭되는 KOPIS Musical 찾기
     */
    private Optional<Musical> findMatchingKopisMusical(CrawledMusicalRanking ranking) {
        String normalizedTitle = normalizeTitle(ranking.getTitle());

        // 모든 KOPIS Musical 조회하여 유사도 기반 매칭
        List<Musical> allMusicals = musicalRepository.findAll();

        return allMusicals.stream()
                .map(m -> new AbstractMap.SimpleEntry<>(
                        m,
                        calculateTitleSimilarity(normalizedTitle, normalizeTitle(m.getTitle()))
                ))
                .filter(entry -> entry.getValue() > 0.7) // 70% 이상 유사도
                .max(Comparator.comparing(Map.Entry::getValue))
                .map(Map.Entry::getKey);
    }

    /**
     * 제목 정규화
     */
    private String normalizeTitle(String title) {
        if (title == null) return "";

        return title
                .replaceAll("[\\[\\]()\\<\\>〈〉]", " ")
                .replaceAll("뮤지컬", "")
                .replaceAll("[^\\w\\s가-힣a-zA-Z0-9]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    /**
     * 제목 유사도 계산 (간단한 버전)
     */
    private double calculateTitleSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;

        // 포함 관계 체크
        if (s1.contains(s2) || s2.contains(s1)) return 0.85;

        // 단어 기반 유사도
        Set<String> words1 = new HashSet<>(Arrays.asList(s1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(s2.split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
}

