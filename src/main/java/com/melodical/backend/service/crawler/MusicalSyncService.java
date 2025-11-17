package com.melodical.backend.service.crawler;

import com.melodical.backend.entity.CrawledMusicalRanking;
import com.melodical.backend.entity.Musical;
import com.melodical.backend.repository.CrawledMusicalRankingRepository;
import com.melodical.backend.repository.MusicalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 크롤링 데이터와 Musical 엔티티를 동기화하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MusicalSyncService {

    private final CrawledMusicalRankingRepository crawledRepository;
    private final MusicalRepository musicalRepository;

    /**
     * 크롤링 데이터를 Musical 엔티티로 동기화
     * - posterUrl을 포함한 모든 정보 업데이트
     */
    @Transactional
    public int syncCrawledDataToMusicals() {
        log.info("Starting to sync crawled data to Musical entities");

        // 최신 월간 데이터 가져오기
        List<CrawledMusicalRanking> rankings = crawledRepository.findLatestByRankingType("MONTHLY");

        int syncCount = 0;
        int createCount = 0;
        int updateCount = 0;

        for (CrawledMusicalRanking ranking : rankings) {
            try {
                // 1순위: Interpark ID로 찾기
                Optional<Musical> existingMusical = findMusicalByInterparkId(ranking.getInterparkId());
                
                // 2순위: 제목으로 찾기
                if (existingMusical.isEmpty()) {
                    existingMusical = findMusicalByTitle(ranking.getTitle());
                }

                if (existingMusical.isPresent()) {
                    // 기존 Musical 업데이트
                    Musical musical = existingMusical.get();
                    updateMusicalFromRanking(musical, ranking);
                    musicalRepository.save(musical);
                    updateCount++;
                    log.debug("Updated Musical: {} with posterUrl: {}",
                             musical.getTitle(), musical.getPosterUrl());
                } else {
                    // 새로운 Musical 생성
                    Musical musical = createMusicalFromRanking(ranking);
                    musicalRepository.save(musical);
                    createCount++;
                    log.debug("Created new Musical: {} with posterUrl: {}",
                             musical.getTitle(), musical.getPosterUrl());
                }
                syncCount++;
            } catch (Exception e) {
                log.error("Failed to sync musical: {}", ranking.getTitle(), e);
            }
        }

        log.info("Sync completed: {} total, {} created, {} updated",
                syncCount, createCount, updateCount);

        return syncCount;
    }

    /**
     * 제목으로 Musical 찾기 (정규화된 비교)
     */
    private Optional<Musical> findMusicalByTitle(String title) {
        List<Musical> allMusicals = musicalRepository.findAll();

        String normalizedTitle = normalizeTitle(title);

        return allMusicals.stream()
                .filter(m -> normalizeTitle(m.getTitle()).equals(normalizedTitle))
                .findFirst();
    }

    /**
     * Interpark ID로 Musical 찾기
     */
    private Optional<Musical> findMusicalByInterparkId(String interparkId) {
        if (interparkId == null || interparkId.isEmpty()) {
            return Optional.empty();
        }
        
        List<Musical> allMusicals = musicalRepository.findAll();
        return allMusicals.stream()
                .filter(m -> interparkId.equals(m.getInterparkId()))
                .findFirst();
    }

    /**
     * 제목 정규화 (비교용)
     */
    private String normalizeTitle(String title) {
        if (title == null) return "";
        // 특수문자, 공백 제거 후 소문자로
        return title.replaceAll("[〈〉《》<>\\[\\]\\(\\)\\s]", "")
                   .toLowerCase();
    }

    /**
     * CrawledMusicalRanking에서 Musical 업데이트
     */
    private void updateMusicalFromRanking(Musical musical, CrawledMusicalRanking ranking) {
        // Interpark ID 업데이트
        if (ranking.getInterparkId() != null) {
            musical.setInterparkId(ranking.getInterparkId());
        }
        
        // posterUrl 업데이트 (가장 중요!)
        if (ranking.getPosterUrl() != null && !ranking.getPosterUrl().trim().isEmpty()) {
            String posterUrl = ranking.getPosterUrl();
            // //로 시작하면 https: 추가
            if (posterUrl.startsWith("//")) {
                posterUrl = "https:" + posterUrl;
            }
            musical.setPosterUrl(posterUrl);
        }

        // 극장 정보
        if (ranking.getTheaterName() != null) {
            musical.setTheater(ranking.getTheaterName());
        }

        // 공연 기간
        if (ranking.getPerformancePeriod() != null) {
            String[] dates = ranking.getPerformancePeriod().split("~");
            if (dates.length >= 1) {
                musical.setStartDate(dates[0].trim());
            }
            if (dates.length >= 2) {
                musical.setEndDate(dates[1].trim());
            }
        }

        // 장르
        if (ranking.getGenre() != null) {
            musical.setGenre(ranking.getGenre());
        }

        // 인기도 점수 (평점 기반)
        if (ranking.getAverageRating() != null) {
            musical.setPopularityScore(ranking.getAverageRating());
        }

        // 판매 여부
        if (ranking.getIsAvailable() != null) {
            musical.setIsOnSale(ranking.getIsAvailable());
        }

        musical.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * CrawledMusicalRanking에서 새 Musical 생성
     */
    private Musical createMusicalFromRanking(CrawledMusicalRanking ranking) {
        Musical musical = new Musical();

        musical.setTitle(ranking.getTitle());
        
        // Interpark ID 설정
        if (ranking.getInterparkId() != null) {
            musical.setInterparkId(ranking.getInterparkId());
        }

        // posterUrl 설정
        if (ranking.getPosterUrl() != null && !ranking.getPosterUrl().trim().isEmpty()) {
            String posterUrl = ranking.getPosterUrl();
            if (posterUrl.startsWith("//")) {
                posterUrl = "https:" + posterUrl;
            }
            musical.setPosterUrl(posterUrl);
        }

        // 극장
        musical.setTheater(ranking.getTheaterName());

        // 공연 기간
        if (ranking.getPerformancePeriod() != null) {
            String[] dates = ranking.getPerformancePeriod().split("~");
            if (dates.length >= 1) {
                musical.setStartDate(dates[0].trim());
            }
            if (dates.length >= 2) {
                musical.setEndDate(dates[1].trim());
            }
        }

        // 장르
        musical.setGenre(ranking.getGenre());

        // 캐스트 정보 (없으면 빈 문자열)
        musical.setCast("");
        musical.setRuntime("");

        // 인기도 점수
        if (ranking.getAverageRating() != null) {
            musical.setPopularityScore(ranking.getAverageRating());
        }

        // 판매 여부
        if (ranking.getIsAvailable() != null) {
            musical.setIsOnSale(ranking.getIsAvailable());
        } else {
            musical.setIsOnSale(true); // 기본값
        }

        return musical;
    }

    /**
     * 특정 Musical의 posterUrl을 크롤링 데이터에서 업데이트
     */
    @Transactional
    public boolean updateMusicalPosterUrl(Long musicalId) {
        Optional<Musical> musicalOpt = musicalRepository.findById(musicalId);
        if (musicalOpt.isEmpty()) {
            log.warn("Musical not found: {}", musicalId);
            return false;
        }

        Musical musical = musicalOpt.get();

        // 제목으로 크롤링 데이터 찾기
        List<CrawledMusicalRanking> rankings = crawledRepository.findLatestByRankingType("MONTHLY");
        String normalizedTitle = normalizeTitle(musical.getTitle());

        Optional<CrawledMusicalRanking> matchingRanking = rankings.stream()
                .filter(r -> normalizeTitle(r.getTitle()).equals(normalizedTitle))
                .findFirst();

        if (matchingRanking.isPresent()) {
            CrawledMusicalRanking ranking = matchingRanking.get();
            if (ranking.getPosterUrl() != null && !ranking.getPosterUrl().trim().isEmpty()) {
                String posterUrl = ranking.getPosterUrl();
                if (posterUrl.startsWith("//")) {
                    posterUrl = "https:" + posterUrl;
                }
                musical.setPosterUrl(posterUrl);
                musicalRepository.save(musical);
                log.info("Updated posterUrl for Musical: {} -> {}",
                        musical.getTitle(), posterUrl);
                return true;
            }
        }

        log.warn("No matching crawled data found for Musical: {}", musical.getTitle());
        return false;
    }
}

