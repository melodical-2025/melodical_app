package com.melodical.backend.config;

import com.melodical.backend.entity.CrawledMusicalRanking;
import com.melodical.backend.entity.Musical;
import com.melodical.backend.repository.CrawledMusicalRankingRepository;
import com.melodical.backend.repository.MusicalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 데이터 정합성을 위한 정리 서비스
 * - 중복된 뮤지컬 데이터 통합
 * - 평점/URL 정보가 없는 뮤지컬에 정보 매칭
 * 
 * ⚠️ TEMPORARILY DISABLED - Causes database lock issues
 * This service locks the Musical table for too long during startup,
 * preventing other operations (like comment insertion) from completing.
 * 
 * TODO: Re-enable with optimizations:
 * - Run in separate thread with lower priority
 * - Use batch processing with smaller chunks
 * - Add proper transaction management
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataCleanupService /* implements CommandLineRunner */ {
    
    private final MusicalRepository musicalRepository;
    private final CrawledMusicalRankingRepository rankingRepository;
    
    // @Override
    public void run(String... args) {
        log.info("🧹 DataCleanupService is currently DISABLED to prevent database lock issues");
        log.info("💡 To enable, uncomment implements CommandLineRunner and @Override annotation");
        
        // TEMPORARILY DISABLED - CAUSES LOCK TIMEOUT ISSUES
        /*
        log.info("🧹 Starting data cleanup process...");
        
        // 1. 먼저 CrawledMusicalRanking 데이터 확인
        checkCrawledDataIntegrity();
        
        // 2. Musical 중복 정리
        cleanupDuplicateMusicals();
        
        log.info("✅ Data cleanup completed");
        */
    }
    
    /**
     * CrawledMusicalRanking 데이터 무결성 확인
     */
    private void checkCrawledDataIntegrity() {
        List<CrawledMusicalRanking> allRankings = rankingRepository.findAll();
        log.info("📊 Total CrawledMusicalRanking records: {}", allRankings.size());
        
        // interparkId로 그룹화하여 중복 확인
        Map<String, List<CrawledMusicalRanking>> groupedByInterparkId = allRankings.stream()
                .filter(r -> r.getInterparkId() != null && !r.getInterparkId().isEmpty())
                .collect(Collectors.groupingBy(CrawledMusicalRanking::getInterparkId));
        
        log.info("📊 Unique interpark IDs in CrawledMusicalRanking: {}", groupedByInterparkId.size());
        
        // 중복 확인
        int duplicatesFound = 0;
        for (Map.Entry<String, List<CrawledMusicalRanking>> entry : groupedByInterparkId.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicatesFound++;
                String interparkId = entry.getKey();
                List<CrawledMusicalRanking> rankings = entry.getValue();
                
                log.warn("⚠️  Duplicate CrawledMusicalRanking for interparkId {}: {} records", 
                        interparkId, rankings.size());
                
                for (CrawledMusicalRanking r : rankings) {
                    log.warn("    - Title: '{}', Rank: {}, Type: {}, Version: {}, URLs: [IP: {}, Y24: {}]", 
                            r.getTitle(), 
                            r.getCombinedRank(),
                            r.getRankingType(),
                            r.getDataVersion(),
                            r.getInterparkUrl() != null ? "O" : "X",
                            r.getYes24Url() != null ? "O" : "X");
                }
            }
        }
        
        if (duplicatesFound > 0) {
            log.warn("⚠️  Found {} interparkIds with duplicate CrawledMusicalRanking records", duplicatesFound);
        } else {
            log.info("✅ No duplicates found in CrawledMusicalRanking");
        }
        
        // URL 통계
        long withInterparkUrl = allRankings.stream()
                .filter(r -> r.getInterparkUrl() != null && !r.getInterparkUrl().isEmpty())
                .count();
        long withYes24Url = allRankings.stream()
                .filter(r -> r.getYes24Url() != null && !r.getYes24Url().isEmpty())
                .count();
        long withBothUrls = allRankings.stream()
                .filter(r -> r.getInterparkUrl() != null && !r.getInterparkUrl().isEmpty() &&
                            r.getYes24Url() != null && !r.getYes24Url().isEmpty())
                .count();
        
        log.info("📊 URL Statistics in CrawledMusicalRanking:");
        log.info("  - With Interpark URL: {}", withInterparkUrl);
        log.info("  - With Yes24 URL: {}", withYes24Url);
        log.info("  - With Both URLs: {}", withBothUrls);
    }
    
    @Transactional
    public void cleanupDuplicateMusicals() {
        List<Musical> allMusicals = musicalRepository.findAll();
        log.info("📊 Total musicals in database: {}", allMusicals.size());
        
        // 1단계: interparkId enrichment - DISABLED due to N+1 performance issues
        // enrichMusicalsWithInterparkId(allMusicals);
        log.info("⚠️  InterparkId enrichment is disabled to avoid performance issues");
        
        // 2단계: interparkId로 그룹화하여 중복 제거
        // allMusicals = musicalRepository.findAll(); // 다시 로드 (enrichment 반영)
        Map<String, List<Musical>> groupedByInterparkId = allMusicals.stream()
                .filter(m -> m.getInterparkId() != null && !m.getInterparkId().isEmpty())
                .collect(Collectors.groupingBy(Musical::getInterparkId));
        
        log.info("📊 Unique interpark IDs: {}", groupedByInterparkId.size());
        
        int duplicatesFound = 0;
        int duplicatesRemoved = 0;
        
        for (Map.Entry<String, List<Musical>> entry : groupedByInterparkId.entrySet()) {
            String interparkId = entry.getKey();
            List<Musical> musicals = entry.getValue();
            
            if (musicals.size() > 1) {
                duplicatesFound++;
                log.warn("🔍 Found {} duplicates for interparkId: {} ({})", 
                        musicals.size(), interparkId, musicals.get(0).getTitle());
                
                // 가장 완전한 데이터를 가진 뮤지컬 선택 (가장 최근 것)
                Musical keepMusical = musicals.stream()
                        .max(Comparator.comparing(Musical::getId))
                        .orElse(musicals.get(0));
                
                // 나머지 삭제
                for (Musical duplicate : musicals) {
                    if (!duplicate.getId().equals(keepMusical.getId())) {
                        log.info("  🗑️  Removing duplicate: id={}, title={}", 
                                duplicate.getId(), duplicate.getTitle());
                        musicalRepository.delete(duplicate);
                        duplicatesRemoved++;
                    }
                }
            }
        }
        
        log.info("📊 Duplicates found: {}", duplicatesFound);
        log.info("📊 Duplicates removed: {}", duplicatesRemoved);
        
        // 평점/URL 정보 매칭 확인
        checkRankingMatching();
    }
    
    /**
     * interparkId가 없는 Musical에 CrawledMusicalRanking과 매칭하여 interparkId 설정
     */
    @Transactional
    public void enrichMusicalsWithInterparkId(List<Musical> musicals) {
        List<CrawledMusicalRanking> allRankings = rankingRepository.findAll();
        
        // 이미 사용 중인 interparkId 수집
        Set<String> usedInterparkIds = musicals.stream()
                .map(Musical::getInterparkId)
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toSet());
        
        // 제목으로 매핑 생성
        Map<String, CrawledMusicalRanking> titleToRanking = new HashMap<>();
        for (CrawledMusicalRanking ranking : allRankings) {
            if (ranking.getInterparkId() != null && !ranking.getInterparkId().isEmpty()) {
                String normalizedTitle = normalizeTitle(ranking.getTitle());
                titleToRanking.put(normalizedTitle, ranking);
            }
        }
        
        int enrichedCount = 0;
        for (Musical musical : musicals) {
            // interparkId가 없거나 비어있는 경우
            if (musical.getInterparkId() == null || musical.getInterparkId().isEmpty()) {
                String normalizedTitle = normalizeTitle(musical.getTitle());
                CrawledMusicalRanking matching = titleToRanking.get(normalizedTitle);
                
                if (matching != null) {
                    String newInterparkId = matching.getInterparkId();
                    
                    // 이미 사용 중인 interparkId는 스킵
                    if (usedInterparkIds.contains(newInterparkId)) {
                        log.warn("⚠️  Skipping Musical '{}': interparkId {} already in use", 
                                musical.getTitle(), newInterparkId);
                        continue;
                    }
                    
                    musical.setInterparkId(newInterparkId);
                    musicalRepository.save(musical);
                    usedInterparkIds.add(newInterparkId); // 사용 중으로 표시
                    enrichedCount++;
                    log.info("✅ Enriched Musical '{}' with interparkId: {}", 
                            musical.getTitle(), newInterparkId);
                } else {
                    log.debug("⚠️  No matching ranking found for Musical: '{}'", musical.getTitle());
                }
            }
        }
        
        log.info("📊 Musicals enriched with interparkId: {}", enrichedCount);
    }
    
    /**
     * 제목 정규화
     */
    private String normalizeTitle(String title) {
        if (title == null) return "";
        return title.replaceAll("[〈〉《》<>\\[\\]\\(\\)\\s뮤지컬]", "")
                   .toLowerCase()
                   .trim();
    }
    
    private void checkRankingMatching() {
        List<Musical> allMusicals = musicalRepository.findAll();
        List<CrawledMusicalRanking> allRankings = rankingRepository.findAll();
        
        log.info("📊 Total rankings in database: {}", allRankings.size());
        
        // interparkId로 그룹화
        Map<String, CrawledMusicalRanking> rankingMap = allRankings.stream()
                .collect(Collectors.toMap(
                        CrawledMusicalRanking::getInterparkId,
                        ranking -> ranking,
                        (r1, r2) -> r1 // 중복 시 첫 번째 선택
                ));
        
        int withRanking = 0;
        int withoutRanking = 0;
        
        for (Musical musical : allMusicals) {
            if (musical.getInterparkId() != null && !musical.getInterparkId().isEmpty()) {
                CrawledMusicalRanking ranking = rankingMap.get(musical.getInterparkId());
                if (ranking != null) {
                    withRanking++;
                    log.debug("✅ Musical '{}' has ranking (IP: {}, Y24: {})", 
                            musical.getTitle(), 
                            ranking.getInterparkRating(), 
                            ranking.getYes24Rating());
                } else {
                    withoutRanking++;
                    log.debug("⚠️  Musical '{}' (interparkId: {}) has NO ranking data", 
                            musical.getTitle(), 
                            musical.getInterparkId());
                }
            }
        }
        
        log.info("📊 Musicals with ranking data: {}", withRanking);
        log.info("📊 Musicals without ranking data: {}", withoutRanking);
    }
}
