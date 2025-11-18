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
     * - 트랜잭션을 개별 처리하여 타임아웃 방지
     */
    public int syncCrawledDataToMusicals() {
        log.info("Starting to sync crawled data to Musical entities");

        // 최신 월간 데이터 가져오기 (읽기 전용)
        List<CrawledMusicalRanking> rankings = crawledRepository.findLatestByRankingType("MONTHLY");
        
        // 전체 Musical 리스트 미리 로드 (성능 최적화)
        List<Musical> allMusicals = musicalRepository.findAll();

        int syncCount = 0;
        int createCount = 0;
        int updateCount = 0;

        for (CrawledMusicalRanking ranking : rankings) {
            try {
                // 각 Musical을 개별 트랜잭션으로 처리
                boolean created = syncSingleMusical(ranking, allMusicals);
                if (created) {
                    createCount++;
                } else {
                    updateCount++;
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
     * 단일 Musical 동기화 (개별 트랜잭션)
     */
    @Transactional
    private boolean syncSingleMusical(CrawledMusicalRanking ranking, List<Musical> allMusicals) {
        // 1순위: Interpark ID로 찾기
        Optional<Musical> existingMusical = findMusicalByInterparkId(ranking.getInterparkId(), allMusicals);
        
        // 2순위: 제목으로 찾기
        if (existingMusical.isEmpty()) {
            existingMusical = findMusicalByTitle(ranking.getTitle(), allMusicals);
        }

        if (existingMusical.isPresent()) {
            // 기존 Musical 업데이트
            Musical musical = existingMusical.get();
            updateMusicalFromRanking(musical, ranking);
            musicalRepository.save(musical);
            log.debug("Updated Musical: {} with posterUrl: {}",
                     musical.getTitle(), musical.getPosterUrl());
            return false; // 업데이트
        } else {
            // 새로운 Musical 생성
            Musical musical = createMusicalFromRanking(ranking);
            musicalRepository.save(musical);
            log.debug("Created new Musical: {} with posterUrl: {}",
                     musical.getTitle(), musical.getPosterUrl());
            return true; // 생성
        }
    }

    /**
     * 제목으로 Musical 찾기 (정규화된 비교)
     */
    private Optional<Musical> findMusicalByTitle(String title, List<Musical> allMusicals) {
        String normalizedTitle = normalizeTitle(title);

        return allMusicals.stream()
                .filter(m -> normalizeTitle(m.getTitle()).equals(normalizedTitle))
                .findFirst();
    }

    /**
     * Interpark ID로 Musical 찾기
     */
    private Optional<Musical> findMusicalByInterparkId(String interparkId, List<Musical> allMusicals) {
        if (interparkId == null || interparkId.isEmpty()) {
            return Optional.empty();
        }
        
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
        
        // URL 업데이트
        if (ranking.getInterparkUrl() != null && !ranking.getInterparkUrl().trim().isEmpty()) {
            musical.setInterparkUrl(ranking.getInterparkUrl());
        }
        if (ranking.getYes24Url() != null && !ranking.getYes24Url().trim().isEmpty()) {
            musical.setYes24Url(ranking.getYes24Url());
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

        // URL 설정
        if (ranking.getInterparkUrl() != null && !ranking.getInterparkUrl().trim().isEmpty()) {
            musical.setInterparkUrl(ranking.getInterparkUrl());
        }
        if (ranking.getYes24Url() != null && !ranking.getYes24Url().trim().isEmpty()) {
            musical.setYes24Url(ranking.getYes24Url());
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

    /**
     * posterUrl이 없는 모든 Musical을 크롤링 데이터로 업데이트
     * - 트랜잭션을 개별 처리하여 타임아웃 방지
     */
    public int fixMissingPosterUrls() {
        log.info("🔍 Starting to fix missing posterUrls...");

        // posterUrl이 없는 모든 Musical 찾기 (읽기 전용)
        List<Musical> musicalsWithoutPoster = musicalRepository.findAll().stream()
                .filter(m -> m.getPosterUrl() == null || m.getPosterUrl().trim().isEmpty())
                .toList();

        log.info("📊 Found {} musicals without posterUrl", musicalsWithoutPoster.size());

        if (musicalsWithoutPoster.isEmpty()) {
            log.info("✅ All musicals have posterUrl!");
            return 0;
        }

        // 최신 크롤링 데이터 가져오기 (읽기 전용)
        List<CrawledMusicalRanking> rankings = crawledRepository.findLatestByRankingType("MONTHLY");
        log.info("📁 Loaded {} crawled rankings", rankings.size());

        int fixedCount = 0;

        for (Musical musical : musicalsWithoutPoster) {
            try {
                // 각 Musical을 개별 트랜잭션으로 처리
                if (fixSingleMusicalPosterUrl(musical, rankings)) {
                    fixedCount++;
                }
            } catch (Exception e) {
                log.error("❌ Failed to fix posterUrl for: {}", musical.getTitle(), e);
            }
        }

        log.info("🎉 Fixed {} out of {} musicals", fixedCount, musicalsWithoutPoster.size());
        return fixedCount;
    }
    
    /**
     * 단일 Musical의 posterUrl 수정 (개별 트랜잭션)
     */
    @Transactional
    private boolean fixSingleMusicalPosterUrl(Musical musical, List<CrawledMusicalRanking> rankings) {
        String normalizedTitle = normalizeTitle(musical.getTitle());

        // 제목으로 매칭되는 크롤링 데이터 찾기
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
                log.info("✅ Fixed posterUrl for '{}': {}", 
                        musical.getTitle(), posterUrl);
                return true;
            } else {
                log.warn("⚠️ Matching ranking found but no posterUrl: {}", musical.getTitle());
            }
        } else {
            log.warn("⚠️ No matching ranking found for: {}", musical.getTitle());
        }
        
        return false;
    }
}

