package com.melodical.backend.service;

import com.melodical.backend.entity.CrawledMusicalRanking;
import com.melodical.backend.entity.Musical;
import com.melodical.backend.repository.CrawledMusicalRankingRepository;
import com.melodical.backend.repository.MusicalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MusicalDataService {
    private final MusicalRepository musicalRepository;
    private final CrawledMusicalRankingRepository crawledRankingRepository;

    /**
     * 월간 인기 뮤지컬 목록 조회
     */
    public List<Map<String, Object>> getMonthlyMusicals() {
        List<Musical> musicals = musicalRepository.findMonthlyMusicals();
        
        // interparkId로 한번에 조회하여 성능 개선
        Set<String> interparkIds = musicals.stream()
                .map(Musical::getInterparkId)
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toSet());
        
        Map<String, CrawledMusicalRanking> rankingMap = new HashMap<>();
        if (!interparkIds.isEmpty()) {
            for (String interparkId : interparkIds) {
                List<CrawledMusicalRanking> rankings = crawledRankingRepository.findByInterparkId(interparkId);
                if (!rankings.isEmpty()) {
                    rankingMap.put(interparkId, rankings.get(0));
                }
            }
        }
        
        return musicals.stream()
                .map(m -> convertToMap(m, rankingMap.get(m.getInterparkId())))
                .collect(Collectors.toList());
    }

    /**
     * 주간 인기 뮤지컬 목록 조회
     */
    public List<Map<String, Object>> getWeeklyMusicals() {
        List<Musical> musicals = musicalRepository.findWeeklyMusicals();
        
        // interparkId로 한번에 조회하여 성능 개선
        Set<String> interparkIds = musicals.stream()
                .map(Musical::getInterparkId)
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toSet());
        
        Map<String, CrawledMusicalRanking> rankingMap = new HashMap<>();
        if (!interparkIds.isEmpty()) {
            for (String interparkId : interparkIds) {
                List<CrawledMusicalRanking> rankings = crawledRankingRepository.findByInterparkId(interparkId);
                if (!rankings.isEmpty()) {
                    rankingMap.put(interparkId, rankings.get(0));
                }
            }
        }
        
        return musicals.stream()
                .map(m -> convertToMap(m, rankingMap.get(m.getInterparkId())))
                .collect(Collectors.toList());
    }

    /**
     * 월간 인기 Top N
     */
    public List<Map<String, Object>> getTopMonthlyMusicals(int count) {
        List<Musical> musicals = musicalRepository.findTopMonthlyMusicals(count);
        
        // interparkId로 한번에 조회하여 성능 개선
        Set<String> interparkIds = musicals.stream()
                .map(Musical::getInterparkId)
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toSet());
        
        Map<String, CrawledMusicalRanking> rankingMap = new HashMap<>();
        if (!interparkIds.isEmpty()) {
            for (String interparkId : interparkIds) {
                List<CrawledMusicalRanking> rankings = crawledRankingRepository.findByInterparkId(interparkId);
                if (!rankings.isEmpty()) {
                    rankingMap.put(interparkId, rankings.get(0));
                }
            }
        }
        
        return musicals.stream()
                .map(m -> convertToMap(m, rankingMap.get(m.getInterparkId())))
                .collect(Collectors.toList());
    }

    /**
     * 통합 검색
     */
    public List<Map<String, Object>> searchMusicals(String query, int limit) {
        List<Musical> musicals = musicalRepository.searchByTitleContaining(query, limit);
        return musicals.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }
    
    /**
     * 뮤지컬 상세 조회 (ID로 조회)
     */
    public Map<String, Object> getMusicalDetail(Long id) {
        Optional<Musical> musicalOpt = musicalRepository.findById(id);
        if (musicalOpt.isEmpty()) {
            return null;
        }
        
        Musical musical = musicalOpt.get();
        CrawledMusicalRanking ranking = findBestMatchingRanking(musical);
        return convertToMap(musical, ranking);
    }
    
    /**
     * 뮤지컬 상세 조회 (interparkId로 조회)
     */
    public Map<String, Object> getMusicalByInterparkId(String interparkId) {
        Musical musical = musicalRepository.findByInterparkId(interparkId);
        if (musical == null) {
            return null;
        }
        
        CrawledMusicalRanking ranking = findBestMatchingRanking(musical);
        return convertToMap(musical, ranking);
    }

    /**
     * Musical 엔티티를 Map으로 변환 (오버로드 - 성능 최적화 버전)
     * @param musical Musical 엔티티
     * @param ranking 미리 조회된 CrawledMusicalRanking (null 가능)
     */
    public Map<String, Object> convertToMap(Musical musical, CrawledMusicalRanking ranking) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", musical.getId());
        map.put("interparkId", musical.getInterparkId());
        map.put("title", musical.getTitle());
        map.put("posterUrl", musical.getPosterUrl());
        map.put("theater", musical.getTheater());
        map.put("startDate", musical.getStartDate());
        map.put("endDate", musical.getEndDate());
        
        // period 정보 추가
        String period = "";
        if (musical.getStartDate() != null && musical.getEndDate() != null) {
            period = musical.getStartDate() + " ~ " + musical.getEndDate();
        }
        map.put("period", period);
        
        map.put("cast", musical.getCast());
        map.put("runtime", musical.getRuntime());
        map.put("genre", musical.getGenre());
        map.put("region", musical.getRegion());
        map.put("priceMin", musical.getPriceMin());
        map.put("priceMax", musical.getPriceMax());
        map.put("isOnSale", musical.getIsOnSale());
        map.put("isNew", musical.getIsNew());
        
        // 미리 조회된 ranking 정보 사용
        if (ranking != null) {
            map.put("interparkRating", ranking.getInterparkRating());
            map.put("yes24Rating", ranking.getYes24Rating());
            map.put("interparkUrl", ranking.getInterparkUrl());
            map.put("yes24Url", ranking.getYes24Url());
        } else {
            map.put("interparkRating", null);
            map.put("yes24Rating", null);
            map.put("interparkUrl", null);
            map.put("yes24Url", null);
        }
        
        return map;
    }
    
    /**
     * Musical 엔티티를 Map으로 변환 (기존 메서드 - 하위 호환성 유지)
     */
    private Map<String, Object> convertToMap(Musical musical) {
        // CrawledMusicalRanking에서 평점과 URL 정보 가져오기
        CrawledMusicalRanking ranking = findBestMatchingRanking(musical);
        return convertToMap(musical, ranking);
    }
    
    /**
     * Musical에 가장 적합한 CrawledMusicalRanking 찾기
     * 우선순위: 1) interparkId 정확 매칭, 2) 제목 유사도 매칭
     */
    private CrawledMusicalRanking findBestMatchingRanking(Musical musical) {
        // 1순위: interparkId로 정확 매칭
        if (musical.getInterparkId() != null && !musical.getInterparkId().isEmpty()) {
            List<CrawledMusicalRanking> rankings = crawledRankingRepository.findByInterparkId(musical.getInterparkId());
            if (!rankings.isEmpty()) {
                // 여러 개 있으면 가장 최신 것 (id가 큰 것) 선택
                return rankings.stream()
                        .max(Comparator.comparing(CrawledMusicalRanking::getId))
                        .orElse(rankings.get(0));
            }
        }
        
        // 2순위: 제목 유사도로 매칭 (interparkId가 없거나 매칭 실패 시)
        String normalizedTitle = normalizeTitle(musical.getTitle());
        List<CrawledMusicalRanking> allRankings = crawledRankingRepository.findAll();
        
        Optional<CrawledMusicalRanking> matchByTitle = allRankings.stream()
                .filter(r -> normalizeTitle(r.getTitle()).equals(normalizedTitle))
                .findFirst();
        
        return matchByTitle.orElse(null);
    }
    
    /**
     * 제목 정규화 (매칭용)
     */
    private String normalizeTitle(String title) {
        if (title == null) return "";
        return title.replaceAll("[〈〉《》<>\\[\\]\\(\\)\\s뮤지컬]", "")
                   .toLowerCase()
                   .trim();
    }
}
