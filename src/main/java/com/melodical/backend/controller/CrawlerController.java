package com.melodical.backend.controller;

import com.melodical.backend.entity.CrawledMusicalRanking;
import com.melodical.backend.service.crawler.CrawledDataService;
import com.melodical.backend.service.crawler.MusicalCrawlerService;
import com.melodical.backend.service.crawler.MusicalSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 크롤링 데이터 관리 API
 */
@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
@Slf4j
public class CrawlerController {

    private final MusicalCrawlerService crawlerService;
    private final CrawledDataService crawledDataService;
    private final MusicalSyncService musicalSyncService;

    /**
     * 수동 크롤링 실행
     */
    @PostMapping("/run")
    public ResponseEntity<Map<String, String>> runCrawler() {
        log.info("Manual crawler execution requested");

        try {
            crawlerService.runCrawlerAndSaveData();

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Crawler executed successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Manual crawler execution failed", e);

            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 주간 인기 뮤지컬 조회
     */
    @GetMapping("/weekly")
    public ResponseEntity<List<CrawledMusicalRanking>> getWeeklyRankings() {
        log.info("Fetching weekly rankings");
        List<CrawledMusicalRanking> rankings = crawledDataService.getWeeklyPopularMusicals();
        return ResponseEntity.ok(rankings);
    }

    /**
     * 월간 인기 뮤지컬 조회
     */
    @GetMapping("/monthly")
    public ResponseEntity<List<CrawledMusicalRanking>> getMonthlyRankings() {
        log.info("Fetching monthly rankings");
        List<CrawledMusicalRanking> rankings = crawledDataService.getMonthlyPopularMusicals();
        return ResponseEntity.ok(rankings);
    }

    /**
     * 주간 상위 N개 조회
     */
    @GetMapping("/weekly/top/{n}")
    public ResponseEntity<List<CrawledMusicalRanking>> getTopWeekly(@PathVariable int n) {
        log.info("Fetching top {} weekly musicals", n);
        List<CrawledMusicalRanking> rankings = crawledDataService.getTopWeeklyMusicals(n);
        return ResponseEntity.ok(rankings);
    }

    /**
     * 월간 상위 N개 조회
     */
    @GetMapping("/monthly/top/{n}")
    public ResponseEntity<List<CrawledMusicalRanking>> getTopMonthly(@PathVariable int n) {
        log.info("Fetching top {} monthly musicals", n);
        List<CrawledMusicalRanking> rankings = crawledDataService.getTopMonthlyMusicals(n);
        return ResponseEntity.ok(rankings);
    }

    /**
     * 예매 가능한 인기 뮤지컬 조회
     */
    @GetMapping("/available/{type}")
    public ResponseEntity<List<CrawledMusicalRanking>> getAvailableMusicals(
            @PathVariable String type) {
        log.info("Fetching available {} musicals", type);

        String rankingType = type.equalsIgnoreCase("weekly") ? "WEEKLY" : "MONTHLY";
        List<CrawledMusicalRanking> rankings =
                crawledDataService.getAvailablePopularMusicals(rankingType);

        return ResponseEntity.ok(rankings);
    }

    /**
     * 크롤링 데이터 통계
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("Fetching crawling statistics");
        Map<String, Object> stats = crawledDataService.getCrawlingStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * 오래된 데이터 정리
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, String>> cleanupOldData(
            @RequestParam(defaultValue = "3") int keepVersions) {
        log.info("Cleanup requested, keeping {} versions", keepVersions);

        try {
            crawlerService.cleanupOldData(keepVersions);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Old data cleaned up successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Cleanup failed", e);

            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * posterUrl이 없는 뮤지컬 복구
     */
    @PostMapping("/fix-poster-urls")
    public ResponseEntity<Map<String, Object>> fixMissingPosterUrls() {
        log.info("🔧 Fix missing posterUrls requested");

        try {
            int fixedCount = musicalSyncService.fixMissingPosterUrls();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Fixed missing posterUrls");
            response.put("fixedCount", fixedCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Failed to fix posterUrls", e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            response.put("fixedCount", 0);

            return ResponseEntity.internalServerError().body(response);
        }
    }
}

