package com.melodical.backend.service;

import com.melodical.backend.dto.MusicalResponseDto;
import com.melodical.backend.entity.CrawledMusicalRanking;
import com.melodical.backend.entity.Musical;
import com.melodical.backend.entity.RatedMusical;
import com.melodical.backend.repository.CrawledMusicalRankingRepository;
import com.melodical.backend.repository.MusicalRepository;
import com.melodical.backend.repository.RatedMusicalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MusicalService {
    private final MusicalRepository repo;
    private final RatedMusicalRepository ratedRepo;
    private final CrawledMusicalRankingRepository rankingRepo;

    public List<MusicalResponseDto> fetchAllMusicals() {
        return repo.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<MusicalResponseDto> fetchRatedMusicals(Long userId) {
        log.info("Fetching rated musicals for user: {}", userId);

        List<RatedMusical> rated = ratedRepo.findByUserId(userId);
        log.info("Found {} rated musicals in database for user {}", rated.size(), userId);

        if (rated.isEmpty()) {
            log.warn("No rated musicals found for user {}", userId);
            return Collections.emptyList();
        }

        // musicalId로 Musical 조회 (개선된 방식)
        List<MusicalResponseDto> result = rated.stream()
                .map(rm -> {
                    String interparkId = rm.getMusicalId();
                    log.debug("Looking up musical with interparkId: {}", interparkId);
                    
                    // interparkId로 직접 조회 (가장 최근 것 하나만)
                    Musical musical = repo.findByInterparkId(interparkId);
                    
                    if (musical == null) {
                        log.warn("Musical not found for interparkId: {}", interparkId);
                        return null;
                    }
                    
                    MusicalResponseDto dto = toDto(musical);
                    log.debug("Mapped Musical: id={}, title={}, interparkRating={}, yes24Rating={}", 
                            dto.getId(), dto.getTitle(), dto.getInterparkRating(), dto.getYes24Rating());
                    return dto;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());

        log.info("Returning {} rated musicals for user {}", result.size(), userId);
        return result;
    }

    private MusicalResponseDto toDto(Musical m) {
        // CrawledMusicalRanking에서 평점과 URL 정보 가져오기
        CrawledMusicalRanking ranking = findBestMatchingRanking(m);
        
        // period 정보 생성
        String period = "";
        if (m.getStartDate() != null && m.getEndDate() != null) {
            period = m.getStartDate() + " ~ " + m.getEndDate();
        }
        
        MusicalResponseDto.MusicalResponseDtoBuilder builder = MusicalResponseDto.builder()
                .id(m.getId())
                .interparkId(m.getInterparkId())
                .cast(m.getCast())
                .endDate(m.getEndDate())
                .posterUrl(m.getPosterUrl())
                .runtime(m.getRuntime())
                .startDate(m.getStartDate())
                .theater(m.getTheater())
                .title(m.getTitle())
                .period(period);
        
        // 평점과 URL 정보 추가 (ranking이 있든 없든 필드는 설정)
        if (ranking != null) {
            builder.interparkRating(ranking.getInterparkRating())
                   .yes24Rating(ranking.getYes24Rating())
                   .interparkUrl(ranking.getInterparkUrl())
                   .yes24Url(ranking.getYes24Url());
            
            log.debug("✅ Musical '{}' (id={}) matched with ranking: IP_URL={}, Y24_URL={}, IP_Rating={}, Y24_Rating={}", 
                    m.getTitle(), m.getId(),
                    ranking.getInterparkUrl() != null ? "O" : "X",
                    ranking.getYes24Url() != null ? "O" : "X",
                    ranking.getInterparkRating(),
                    ranking.getYes24Rating());
        } else {
            // ranking이 없어도 null 필드로라도 설정
            builder.interparkRating(null)
                   .yes24Rating(null)
                   .interparkUrl(null)
                   .yes24Url(null);
            
            log.warn("⚠️  No ranking data found for Musical '{}' (id={}, interparkId={})", 
                    m.getTitle(), m.getId(), m.getInterparkId());
        }
        
        return builder.build();
    }
    
    /**
     * Musical에 가장 적합한 CrawledMusicalRanking 찾기
     * 우선순위: 1) interparkId 정확 매칭, 2) 제목 유사도 매칭
     */
    private CrawledMusicalRanking findBestMatchingRanking(Musical musical) {
        // 1순위: interparkId로 정확 매칭
        if (musical.getInterparkId() != null && !musical.getInterparkId().isEmpty()) {
            List<CrawledMusicalRanking> rankings = rankingRepo.findByInterparkId(musical.getInterparkId());
            if (!rankings.isEmpty()) {
                // 여러 개 있으면 가장 최신 것 (id가 큰 것) 선택
                CrawledMusicalRanking bestRanking = rankings.stream()
                        .max(java.util.Comparator.comparing(CrawledMusicalRanking::getId))
                        .orElse(rankings.get(0));
                
                log.debug("Found ranking by interparkId '{}' for musical '{}'", 
                        musical.getInterparkId(), musical.getTitle());
                return bestRanking;
            }
        }
        
        // 2순위: 제목 유사도로 매칭 (interparkId가 없거나 매칭 실패 시)
        String normalizedTitle = normalizeTitle(musical.getTitle());
        List<CrawledMusicalRanking> allRankings = rankingRepo.findAll();
        
        java.util.Optional<CrawledMusicalRanking> matchByTitle = allRankings.stream()
                .filter(r -> normalizeTitle(r.getTitle()).equals(normalizedTitle))
                .findFirst();
        
        if (matchByTitle.isPresent()) {
            log.debug("Found ranking by title match for musical '{}'", musical.getTitle());
            return matchByTitle.get();
        }
        
        log.debug("No ranking found for musical '{}' (interparkId: {})", 
                musical.getTitle(), musical.getInterparkId());
        return null;
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
