package com.melodical.backend.service;

import com.melodical.backend.dto.MusicalRatingDto;
import com.melodical.backend.dto.MusicalRatingRequest;
import com.melodical.backend.entity.CrawledMusicalRanking;
import com.melodical.backend.entity.Musical;
import com.melodical.backend.entity.RatedMusical;
import com.melodical.backend.entity.User;
import com.melodical.backend.repository.CrawledMusicalRankingRepository;
import com.melodical.backend.repository.MusicalRepository;
import com.melodical.backend.repository.RatedMusicalRepository;
import com.melodical.backend.repository.UserRepository;
import com.melodical.backend.service.recommendation.ProactiveRecommendationService;
import com.melodical.backend.service.recommendation.MusicalFanProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MusicalRatingService {
    private final RatedMusicalRepository ratedMusicalRepo;
    private final UserRepository userRepo;
    private final MusicalRepository musicalRepo;
    private final CrawledMusicalRankingRepository crawledRepo;
    private final MusicalFanProfileService musicalFanProfileService;
    private final ProactiveRecommendationService proactiveRecommendationService;

    @Transactional
    public void saveRatings(MusicalRatingRequest req) {
        Long userId = req.getUserId();
        User userEntity = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid userId: " + userId));

        log.info("Saving ratings for user: {}, count: {}", userId, req.getMusicalRatings().size());

        for (MusicalRatingDto dto : req.getMusicalRatings()) {
            Long musicalId = Long.valueOf(dto.getMusicalId());
            Double score = dto.getRating();

            log.info("Processing musicalId: {}, rating: {}", musicalId, score);

            // 1. CrawledMusicalRanking에서 찾기
            Optional<CrawledMusicalRanking> crawledOpt = crawledRepo.findById(musicalId);

            final Musical musicalEntity;

            if (crawledOpt.isPresent()) {
                CrawledMusicalRanking crawled = crawledOpt.get();
                log.info("Found CrawledMusicalRanking: {}", crawled.getTitle());

                // 2. 제목으로 Musical 찾기
                String normalizedTitle = normalizeTitle(crawled.getTitle());
                Optional<Musical> musicalOpt = musicalRepo.findAll().stream()
                        .filter(m -> normalizeTitle(m.getTitle()).equals(normalizedTitle))
                        .findFirst();

                if (musicalOpt.isPresent()) {
                    musicalEntity = musicalOpt.get();
                    log.info("Found existing Musical: {}", musicalEntity.getTitle());
                } else {
                    // 3. Musical이 없으면 생성
                    Musical newMusical = createMusicalFromCrawled(crawled);
                    musicalEntity = musicalRepo.save(newMusical);
                    log.info("Created new Musical: {}", musicalEntity.getTitle());
                }
            } else {
                // CrawledMusicalRanking도 없으면 Musical ID로 직접 찾기 (fallback)
                musicalEntity = musicalRepo.findById(musicalId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Neither CrawledMusicalRanking nor Musical found for id: " + musicalId));
                log.info("Found Musical directly: {}", musicalEntity.getTitle());
            }

            // 4. RatedMusical 저장 (interparkId 사용)
            final String interparkId;
            if (musicalEntity.getInterparkId() != null && !musicalEntity.getInterparkId().isEmpty()) {
                interparkId = musicalEntity.getInterparkId();
            } else {
                // interparkId가 없으면 Musical ID를 문자열로 사용
                interparkId = musicalEntity.getId().toString();
                log.warn("Musical {} has no interparkId, using id: {}", musicalEntity.getTitle(), interparkId);
            }
            
            RatedMusical record = ratedMusicalRepo
                    .findByUserIdAndMusicalId(userId, interparkId)
                    .orElseGet(() -> RatedMusical.builder()
                            .user(userEntity)
                            .musicalId(interparkId)
                            .build());

            record.setRating(score);
            record.setRatedAt(LocalDateTime.now());
            ratedMusicalRepo.save(record);

            log.info("Saved rating for Musical: {} (id: {}), rating: {}",
                    musicalEntity.getTitle(), musicalEntity.getId(), score);
            
            // 뮤지컬 팬 프로필 업데이트 (비동기로 처리)
            try {
                musicalFanProfileService.updateMusicalFanProfile(musicalEntity);
            } catch (Exception e) {
                log.warn("Failed to update musical fan profile, but continuing: {}", e.getMessage());
            }
        }

        log.info("Successfully saved {} ratings for user {}", req.getMusicalRatings().size(), userId);
        
        // 사용자의 추천 목록 재생성 (비동기)
        try {
            proactiveRecommendationService.refreshRecommendations(userId);
            log.info("✅ Triggered recommendation refresh for user: {}", userId);
        } catch (Exception e) {
            log.warn("Failed to trigger recommendation refresh: {}", e.getMessage());
        }
    }

    /**
     * CrawledMusicalRanking에서 Musical 생성
     */
    private Musical createMusicalFromCrawled(CrawledMusicalRanking crawled) {
        Musical musical = new Musical();
        musical.setTitle(crawled.getTitle());

        // posterUrl 처리
        String posterUrl = crawled.getPosterUrl();
        if (posterUrl != null && posterUrl.startsWith("//")) {
            posterUrl = "https:" + posterUrl;
        }
        musical.setPosterUrl(posterUrl);

        musical.setTheater(crawled.getTheaterName());

        // 공연 기간 파싱
        if (crawled.getPerformancePeriod() != null) {
            String[] dates = crawled.getPerformancePeriod().split("~");
            if (dates.length >= 1) {
                musical.setStartDate(dates[0].trim());
            }
            if (dates.length >= 2) {
                musical.setEndDate(dates[1].trim());
            }
        }

        musical.setGenre(crawled.getGenre());
        musical.setCast("");
        musical.setRuntime("");

        if (crawled.getAverageRating() != null) {
            musical.setPopularityScore(crawled.getAverageRating());
        }

        if (crawled.getIsAvailable() != null) {
            musical.setIsOnSale(crawled.getIsAvailable());
        } else {
            musical.setIsOnSale(true);
        }

        return musical;
    }

    /**
     * 제목 정규화
     */
    private String normalizeTitle(String title) {
        if (title == null) return "";
        return title.replaceAll("[〈〉《》<>\\[\\]\\(\\)\\s]", "")
                   .toLowerCase();
    }

    /**
     * userId 가 평가한 뮤지컬을 Map<musicalId, rating> 으로 조회
     */
    public Map<String, Double> findUserRatings(Long userId) {
        List<RatedMusical> list = ratedMusicalRepo.findByUserId(userId);

        Map<String, Double> map = new HashMap<>();
        for (RatedMusical r : list) {
            // musicalId는 이제 String (interparkId)
            map.put(r.getMusicalId(), r.getRating());
        }
        return map;
    }

    /**
     * 일괄 평점 저장 (Map 형식) - MusicalController용
     * List<Map<String, Object>> 형식으로 받아서 처리
     */
    @Transactional
    public void saveBatchRatings(List<Map<String, Object>> ratings) {
        log.info("📊 Batch saving {} ratings", ratings.size());
        
        for (Map<String, Object> ratingMap : ratings) {
            try {
                // userId와 musicalId, rating 추출
                Long userId = Long.valueOf(ratingMap.get("userId").toString());
                Object musicalIdObj = ratingMap.get("musicalId");
                Double score = Double.valueOf(ratingMap.get("rating").toString());
                
                log.debug("Processing rating: userId={}, musicalId={}, rating={}", userId, musicalIdObj, score);
                
                // User 조회
                User userEntity = userRepo.findById(userId)
                        .orElseThrow(() -> {
                            log.error("❌ User not found: userId={}", userId);
                            return new IllegalArgumentException("Invalid userId: " + userId);
                        });
                
                log.debug("✅ User found: {}", userEntity.getEmail());
                
                // musicalId를 interparkId로 변환
                final String interparkId = convertToInterparkId(musicalIdObj);
                
                // 기존 평점 확인 또는 새로 생성 (interparkId로 조회)
                RatedMusical record = ratedMusicalRepo
                        .findByUserIdAndMusicalId(userId, interparkId)
                        .orElseGet(() -> {
                            log.debug("Creating new rating record for interparkId: {}", interparkId);
                            return RatedMusical.builder()
                                    .user(userEntity)
                                    .musicalId(interparkId)
                                    .build();
                        });
                
                record.setRating(score);
                record.setRatedAt(LocalDateTime.now());
                ratedMusicalRepo.save(record);
                
                log.info("✅ Saved rating: userId={}, interparkId={}, rating={}", userId, interparkId, score);
            } catch (IllegalArgumentException e) {
                log.error("❌ Invalid data in rating: {}", ratingMap, e);
                throw e; // 잘못된 데이터는 즉시 실패
            } catch (Exception e) {
                log.error("❌ Unexpected error saving rating: {}", ratingMap, e);
                throw new RuntimeException("평점 저장 실패: " + e.getMessage(), e);
            }
        }
        
        log.info("✅ Batch save completed successfully");
        
        // 평점을 저장한 사용자의 추천 목록 재생성 (비동기)
        if (!ratings.isEmpty()) {
            try {
                Object userIdObj = ratings.get(0).get("userId");
                Long userId = Long.valueOf(userIdObj.toString());
                proactiveRecommendationService.refreshRecommendations(userId);
                log.info("✅ Triggered recommendation refresh for user: {}", userId);
            } catch (Exception e) {
                log.warn("Failed to trigger recommendation refresh: {}", e.getMessage());
            }
        }
    }
    
    /**
     * musicalId를 interparkId(String)로 변환
     * - musicalId가 Long인 경우: Musical 조회 후 interparkId 추출
     * - Musical이 없으면 CrawledMusicalRanking에서 찾아서 Musical 생성
     * - musicalId가 String인 경우: 그대로 사용
     */
    private String convertToInterparkId(Object musicalIdObj) {
        try {
            Long musicalDbId = Long.valueOf(musicalIdObj.toString());
            
            // 1. Musical에서 찾기
            Optional<Musical> musicalOpt = musicalRepo.findById(musicalDbId);
            
            if (musicalOpt.isPresent()) {
                Musical musical = musicalOpt.get();
                String interparkId = musical.getInterparkId();
                if (interparkId == null || interparkId.isEmpty()) {
                    log.warn("⚠️ Musical {} has no interparkId, using id as fallback", musicalDbId);
                    return musicalDbId.toString();
                }
                log.debug("✅ Musical found: title={}, interparkId={}", musical.getTitle(), interparkId);
                return interparkId;
            }
            
            // 2. Musical이 없으면 CrawledMusicalRanking에서 찾기
            Optional<CrawledMusicalRanking> crawledOpt = crawledRepo.findById(musicalDbId);
            
            if (crawledOpt.isPresent()) {
                CrawledMusicalRanking crawled = crawledOpt.get();
                log.info("✅ Found CrawledMusicalRanking: {} (id: {})", crawled.getTitle(), musicalDbId);
                
                // 제목으로 기존 Musical이 있는지 확인
                String normalizedTitle = normalizeTitle(crawled.getTitle());
                Optional<Musical> existingMusical = musicalRepo.findAll().stream()
                        .filter(m -> normalizeTitle(m.getTitle()).equals(normalizedTitle))
                        .findFirst();
                
                Musical musical;
                if (existingMusical.isPresent()) {
                    musical = existingMusical.get();
                    log.info("✅ Found existing Musical by title: {}", musical.getTitle());
                } else {
                    // Musical 생성
                    musical = createMusicalFromCrawled(crawled);
                    musical = musicalRepo.save(musical);
                    log.info("✅ Created new Musical: {} (id: {})", musical.getTitle(), musical.getId());
                }
                
                String interparkId = musical.getInterparkId();
                if (interparkId == null || interparkId.isEmpty()) {
                    // interparkId가 없으면 crawled ID를 문자열로 사용
                    interparkId = musicalDbId.toString();
                    log.warn("⚠️ Musical has no interparkId, using crawled id: {}", interparkId);
                }
                
                return interparkId;
            }
            
            // 3. 둘 다 없으면 에러
            log.error("❌ Neither Musical nor CrawledMusicalRanking found for id: {}", musicalDbId);
            throw new IllegalArgumentException("Invalid musicalId: " + musicalDbId);
            
        } catch (NumberFormatException e) {
            // musicalId가 이미 String인 경우 (interparkId)
            String interparkId = musicalIdObj.toString();
            log.debug("Using musicalId as interparkId: {}", interparkId);
            return interparkId;
        }
    }
    
    /**
     * 사용자가 평가한 뮤지컬 ID와 평점 Map 조회
     * @param userId 사용자 ID
     * @return Map<Musical DB ID, Rating>
     */
    public Map<Long, Double> getMyRatings(Long userId) {
        log.info("🔍 Fetching ratings for user: {}", userId);
        
        List<RatedMusical> ratedMusicals = ratedMusicalRepo.findByUserId(userId);
        log.info("✅ Found {} rated musicals for user {}", ratedMusicals.size(), userId);
        
        Map<Long, Double> ratingsMap = new HashMap<>();
        
        for (RatedMusical rated : ratedMusicals) {
            String interparkId = rated.getMusicalId();
            
            // interparkId로 Musical 찾기
            List<Musical> musicals = musicalRepo.findAll().stream()
                    .filter(m -> interparkId.equals(m.getInterparkId()))
                    .toList();
            
            if (!musicals.isEmpty()) {
                Musical musical = musicals.get(0);
                ratingsMap.put(musical.getId(), rated.getRating());
                log.debug("📊 Musical ID: {}, Title: {}, Rating: {}", 
                        musical.getId(), musical.getTitle(), rated.getRating());
            } else {
                log.warn("⚠️ No musical found for interparkId: {}", interparkId);
            }
        }
        
        return ratingsMap;
    }
}
