package com.melodical.backend.controller;

import com.melodical.backend.entity.CrawledMusicalRanking;
import com.melodical.backend.entity.Musical;
import com.melodical.backend.repository.CrawledMusicalRankingRepository;
import com.melodical.backend.repository.MusicalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 데이터 진단을 위한 임시 컨트롤러
 * 개발/디버깅 목적으로 사용
 */
@RestController
@RequestMapping("/api/diagnostics")
@RequiredArgsConstructor
@Slf4j
public class DataDiagnosticsController {

    private final MusicalRepository musicalRepo;
    private final CrawledMusicalRankingRepository rankingRepo;

    /**
     * Musical 테이블의 중복 확인
     */
    @GetMapping("/musicals/duplicates")
    public Map<String, Object> checkMusicalDuplicates() {
        List<Musical> allMusicals = musicalRepo.findAll();
        
        // interparkId로 그룹화
        Map<String, List<Musical>> groupedByInterparkId = allMusicals.stream()
                .filter(m -> m.getInterparkId() != null && !m.getInterparkId().isEmpty())
                .collect(Collectors.groupingBy(Musical::getInterparkId));
        
        // 중복 찾기
        List<Map<String, Object>> duplicates = new ArrayList<>();
        for (Map.Entry<String, List<Musical>> entry : groupedByInterparkId.entrySet()) {
            if (entry.getValue().size() > 1) {
                Map<String, Object> duplicateInfo = new HashMap<>();
                duplicateInfo.put("interparkId", entry.getKey());
                duplicateInfo.put("count", entry.getValue().size());
                duplicateInfo.put("musicals", entry.getValue().stream()
                        .map(m -> Map.of(
                                "id", m.getId(),
                                "title", m.getTitle(),
                                "posterUrl", m.getPosterUrl() != null ? m.getPosterUrl() : "null",
                                "theater", m.getTheater() != null ? m.getTheater() : "null"
                        ))
                        .collect(Collectors.toList()));
                duplicates.add(duplicateInfo);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalMusicals", allMusicals.size());
        result.put("uniqueInterparkIds", groupedByInterparkId.size());
        result.put("duplicateInterparkIds", duplicates.size());
        result.put("duplicates", duplicates);
        
        return result;
    }

    /**
     * CrawledMusicalRanking 테이블의 중복 확인
     */
    @GetMapping("/rankings/duplicates")
    public Map<String, Object> checkRankingDuplicates() {
        List<CrawledMusicalRanking> allRankings = rankingRepo.findAll();
        
        // interparkId로 그룹화
        Map<String, List<CrawledMusicalRanking>> groupedByInterparkId = allRankings.stream()
                .filter(r -> r.getInterparkId() != null && !r.getInterparkId().isEmpty())
                .collect(Collectors.groupingBy(CrawledMusicalRanking::getInterparkId));
        
        // 중복 찾기
        List<Map<String, Object>> duplicates = new ArrayList<>();
        for (Map.Entry<String, List<CrawledMusicalRanking>> entry : groupedByInterparkId.entrySet()) {
            if (entry.getValue().size() > 1) {
                Map<String, Object> duplicateInfo = new HashMap<>();
                duplicateInfo.put("interparkId", entry.getKey());
                duplicateInfo.put("count", entry.getValue().size());
                duplicateInfo.put("rankings", entry.getValue().stream()
                        .map(r -> Map.of(
                                "id", r.getId(),
                                "title", r.getTitle(),
                                "rankingType", r.getRankingType(),
                                "dataVersion", r.getDataVersion() != null ? r.getDataVersion() : "null",
                                "interparkUrl", r.getInterparkUrl() != null ? "present" : "null",
                                "yes24Url", r.getYes24Url() != null ? "present" : "null",
                                "interparkRating", r.getInterparkRating() != null ? r.getInterparkRating() : "null",
                                "yes24Rating", r.getYes24Rating() != null ? r.getYes24Rating() : "null"
                        ))
                        .collect(Collectors.toList()));
                duplicates.add(duplicateInfo);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalRankings", allRankings.size());
        result.put("uniqueInterparkIds", groupedByInterparkId.size());
        result.put("duplicateInterparkIds", duplicates.size());
        result.put("duplicates", duplicates);
        
        return result;
    }

    /**
     * 특정 interparkId에 대한 상세 정보
     */
    @GetMapping("/interpark/{interparkId}")
    public Map<String, Object> checkInterparkId(@PathVariable String interparkId) {
        List<Musical> musicals = musicalRepo.findAllByInterparkId(interparkId);
        List<CrawledMusicalRanking> rankings = rankingRepo.findByInterparkId(interparkId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("interparkId", interparkId);
        result.put("musicalsCount", musicals.size());
        result.put("rankingsCount", rankings.size());
        
        result.put("musicals", musicals.stream()
                .map(m -> Map.of(
                        "id", m.getId(),
                        "title", m.getTitle(),
                        "posterUrl", m.getPosterUrl() != null ? m.getPosterUrl() : "null",
                        "theater", m.getTheater() != null ? m.getTheater() : "null",
                        "period", (m.getStartDate() != null ? m.getStartDate() : "") + " ~ " + 
                                 (m.getEndDate() != null ? m.getEndDate() : "")
                ))
                .collect(Collectors.toList()));
        
        result.put("rankings", rankings.stream()
                .map(r -> Map.of(
                        "id", r.getId(),
                        "title", r.getTitle(),
                        "rankingType", r.getRankingType(),
                        "dataVersion", r.getDataVersion() != null ? r.getDataVersion() : "null",
                        "interparkUrl", r.getInterparkUrl() != null ? r.getInterparkUrl() : "null",
                        "yes24Url", r.getYes24Url() != null ? r.getYes24Url() : "null",
                        "interparkRating", r.getInterparkRating() != null ? r.getInterparkRating() : "null",
                        "yes24Rating", r.getYes24Rating() != null ? r.getYes24Rating() : "null"
                ))
                .collect(Collectors.toList()));
        
        return result;
    }

    /**
     * 데이터 통계 전체 조회
     */
    @GetMapping("/stats")
    public Map<String, Object> getDataStatistics() {
        List<Musical> allMusicals = musicalRepo.findAll();
        List<CrawledMusicalRanking> allRankings = rankingRepo.findAll();
        
        Map<String, Object> stats = new HashMap<>();
        
        // Musical 통계
        stats.put("totalMusicals", allMusicals.size());
        stats.put("musicalsWithInterparkId", allMusicals.stream()
                .filter(m -> m.getInterparkId() != null && !m.getInterparkId().isEmpty())
                .count());
        stats.put("musicalsWithPosterUrl", allMusicals.stream()
                .filter(m -> m.getPosterUrl() != null && !m.getPosterUrl().isEmpty())
                .count());
        
        // CrawledMusicalRanking 통계
        stats.put("totalRankings", allRankings.size());
        stats.put("rankingsWithInterparkUrl", allRankings.stream()
                .filter(r -> r.getInterparkUrl() != null && !r.getInterparkUrl().isEmpty())
                .count());
        stats.put("rankingsWithYes24Url", allRankings.stream()
                .filter(r -> r.getYes24Url() != null && !r.getYes24Url().isEmpty())
                .count());
        stats.put("rankingsWithBothUrls", allRankings.stream()
                .filter(r -> r.getInterparkUrl() != null && !r.getInterparkUrl().isEmpty() &&
                            r.getYes24Url() != null && !r.getYes24Url().isEmpty())
                .count());
        
        // 데이터 버전 정보
        Map<String, Long> versionCounts = allRankings.stream()
                .filter(r -> r.getDataVersion() != null)
                .collect(Collectors.groupingBy(
                        CrawledMusicalRanking::getDataVersion,
                        Collectors.counting()
                ));
        stats.put("dataVersions", versionCounts);
        
        return stats;
    }
}
