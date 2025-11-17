package com.melodical.backend.service.crawler;

import com.melodical.backend.entity.CrawledMusicalRanking;
import com.melodical.backend.entity.Musical;
import com.melodical.backend.repository.CrawledMusicalRankingRepository;
import com.melodical.backend.repository.MusicalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 크롤링 데이터를 추천 시스템에 활용하는 서비스
 * KOPIS API 데이터, integrated_weekly_dataset, integrated_monthly_dataset 통합
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CrawledDataService {

    private final CrawledMusicalRankingRepository crawledRepository;
    private final MusicalRepository musicalRepository;

    /**
     * 주간 인기 뮤지컬 조회
     */
    public List<CrawledMusicalRanking> getWeeklyPopularMusicals() {
        return crawledRepository.findLatestByRankingType("WEEKLY");
    }

    /**
     * 월간 인기 뮤지컬 조회
     */
    public List<CrawledMusicalRanking> getMonthlyPopularMusicals() {
        return crawledRepository.findLatestByRankingType("MONTHLY");
    }

    /**
     * 주간 인기 뮤지컬 (상위 N개)
     */
    public List<CrawledMusicalRanking> getTopWeeklyMusicals(int topN) {
        List<CrawledMusicalRanking> weekly = getWeeklyPopularMusicals();
        return weekly.stream()
                .limit(topN)
                .collect(Collectors.toList());
    }

    /**
     * 월간 인기 뮤지컬 (상위 N개)
     */
    public List<CrawledMusicalRanking> getTopMonthlyMusicals(int topN) {
        List<CrawledMusicalRanking> monthly = getMonthlyPopularMusicals();
        return monthly.stream()
                .limit(topN)
                .collect(Collectors.toList());
    }

    /**
     * 예매 가능한 인기 뮤지컬 조회
     */
    public List<CrawledMusicalRanking> getAvailablePopularMusicals(String rankingType) {
        return crawledRepository.findAvailableByRankingType(rankingType);
    }

    /**
     * 크롤링 데이터 기반 인기도 점수 계산
     * 순위, 평점, 리뷰 수를 종합하여 0-1 사이 점수 반환
     */
    public double calculatePopularityScore(CrawledMusicalRanking ranking) {
        double rankScore = 0.0;
        double ratingScore = 0.0;
        double reviewScore = 0.0;

        // 1. 순위 점수 (1위 = 1.0, 순위가 낮을수록 감소)
        if (ranking.getCombinedRank() != null) {
            rankScore = Math.max(0, 1.0 - (ranking.getCombinedRank() - 1) * 0.02);
        }

        // 2. 평점 점수 (10점 만점 기준 -> 0-1 정규화)
        if (ranking.getAverageRating() != null) {
            ratingScore = ranking.getAverageRating() / 10.0;
        }

        // 3. 리뷰 수 점수 (로그 스케일, 1000개 이상 = 1.0)
        if (ranking.getTotalReviews() != null && ranking.getTotalReviews() > 0) {
            reviewScore = Math.min(1.0, Math.log10(ranking.getTotalReviews() + 1) / 3.0);
        }

        // 가중 평균 (순위 50%, 평점 30%, 리뷰 20%)
        return rankScore * 0.5 + ratingScore * 0.3 + reviewScore * 0.2;
    }

    /**
     * KOPIS Musical과 크롤링 데이터 매칭
     * 제목 유사도 기반 매칭
     */
    @Transactional(readOnly = true)
    public Optional<CrawledMusicalRanking> findMatchingCrawledData(Musical musical) {
        try {
            String normalizedTitle = normalizeTitle(musical.getTitle());

            // 먼저 주간 데이터에서 검색
            List<CrawledMusicalRanking> weeklyData = getWeeklyPopularMusicals();
            Optional<CrawledMusicalRanking> weeklyMatch = findBestMatch(normalizedTitle, weeklyData);

            if (weeklyMatch.isPresent()) {
                return weeklyMatch;
            }

            // 주간에 없으면 월간 데이터에서 검색
            List<CrawledMusicalRanking> monthlyData = getMonthlyPopularMusicals();
            return findBestMatch(normalizedTitle, monthlyData);

        } catch (Exception e) {
            log.error("Error matching crawled data for musical: {}", musical.getTitle(), e);
            return Optional.empty();
        }
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
     * 최적 매칭 찾기 (제목 유사도 기반)
     */
    private Optional<CrawledMusicalRanking> findBestMatch(String normalizedTitle,
                                                          List<CrawledMusicalRanking> candidates) {
        if (normalizedTitle.isEmpty() || candidates.isEmpty()) {
            return Optional.empty();
        }

        return candidates.stream()
                .filter(c -> c.getNormalizedTitle() != null)
                .map(c -> new AbstractMap.SimpleEntry<>(
                        c,
                        calculateSimilarity(normalizedTitle, c.getNormalizedTitle())
                ))
                .filter(entry -> entry.getValue() > 0.7) // 70% 이상 유사도
                .max(Comparator.comparing(Map.Entry::getValue))
                .map(Map.Entry::getKey);
    }

    /**
     * 문자열 유사도 계산 (Levenshtein distance 기반)
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;

        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;

        int distance = levenshteinDistance(s1.toLowerCase(), s2.toLowerCase());
        return 1.0 - (double) distance / maxLength;
    }

    /**
     * Levenshtein distance 계산
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * 크롤링 데이터 통계 조회
     */
    public Map<String, Object> getCrawlingStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 주간 데이터 통계
            List<CrawledMusicalRanking> weeklyData = getWeeklyPopularMusicals();
            stats.put("weekly_count", weeklyData.size());
            stats.put("weekly_latest_version",
                    crawledRepository.findLatestDataVersion("WEEKLY").orElse("N/A"));

            // 월간 데이터 통계
            List<CrawledMusicalRanking> monthlyData = getMonthlyPopularMusicals();
            stats.put("monthly_count", monthlyData.size());
            stats.put("monthly_latest_version",
                    crawledRepository.findLatestDataVersion("MONTHLY").orElse("N/A"));

            // 예매 가능한 뮤지컬 수
            long availableWeekly = weeklyData.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getIsAvailable()))
                    .count();
            stats.put("available_weekly", availableWeekly);

            long availableMonthly = monthlyData.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getIsAvailable()))
                    .count();
            stats.put("available_monthly", availableMonthly);

            // 평균 평점
            double avgRating = weeklyData.stream()
                    .filter(r -> r.getAverageRating() != null)
                    .mapToDouble(CrawledMusicalRanking::getAverageRating)
                    .average()
                    .orElse(0.0);
            stats.put("average_rating", avgRating);

        } catch (Exception e) {
            log.error("Error calculating crawling statistics", e);
        }

        return stats;
    }

    /**
     * 특정 순위 범위의 뮤지컬 ID 조회 (추천 시스템용)
     */
    public List<Long> getPopularMusicalIds(String rankingType, int topN) {
        List<CrawledMusicalRanking> rankings = rankingType.equals("WEEKLY")
                ? getTopWeeklyMusicals(topN)
                : getTopMonthlyMusicals(topN);

        // KOPIS Musical과 매칭 시도
        return rankings.stream()
                .map(this::findKopisMusicalId)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * 크롤링 데이터와 매칭되는 KOPIS Musical ID 찾기
     */
    private Optional<Long> findKopisMusicalId(CrawledMusicalRanking ranking) {
        String normalizedTitle = ranking.getNormalizedTitle();
        if (normalizedTitle == null) return Optional.empty();

        // KOPIS DB에서 유사한 제목 검색
        List<Musical> allMusicals = musicalRepository.findAll();

        return allMusicals.stream()
                .map(m -> new AbstractMap.SimpleEntry<>(
                        m,
                        calculateSimilarity(normalizedTitle, normalizeTitle(m.getTitle()))
                ))
                .filter(entry -> entry.getValue() > 0.7)
                .max(Comparator.comparing(Map.Entry::getValue))
                .map(entry -> entry.getKey().getId());
    }
}

