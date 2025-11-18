package com.melodical.backend.service;

import com.melodical.backend.dto.*;
import com.melodical.backend.entity.Rated;
import com.melodical.backend.repository.RatedRepository;
import com.melodical.backend.service.recommendation.ProactiveRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MusicRatingService {

    private final RatedRepository ratedRepo;
    private final ProactiveRecommendationService proactiveRecommendationService;

    /** 사용자 + 곡별 평점을 저장(또는 업데이트) */
    public void saveRatings(MusicRatingRequest req) {
        String contentType = req.getType();  // "music" 등
        Long userId = req.getUserId();
        
        for (RatingDto r : req.getMusicRatings()) {
            String contentId = r.getContentId();
            double rating = r.getRating();

            Rated record = ratedRepo
                    .findByUserIdAndContentTypeAndContentId(userId, contentType, contentId)
                    .orElseGet(() -> Rated.builder()
                            .userId(userId)
                            .contentType(contentType)
                            .contentId(contentId)
                            .build()
                    );

            // 만약 title/artist 가 DTO 에 있으면 업데이트,
            // 아니면 기존 DB 값을 유지하도록 분기해도 됩니다.
            record.setTitle(r.getTitle());
            record.setArtist(r.getArtist());
            record.setRating(rating);
            record.setRatedAt(LocalDateTime.now());

            ratedRepo.save(record);
            
            // Note: 사용자 음악 프로필은 배치 작업으로 업데이트됨
            // UserProfileService의 scheduled 작업 참조
        }
        
        // 사용자의 추천 목록 재생성 (비동기)
        try {
            proactiveRecommendationService.refreshRecommendations(userId);
        } catch (Exception e) {
            // 추천 재생성 실패는 로그만 남기고 무시
        }
    }

    /** 사용자가 매긴 평점을 Map<contentId, rating> 형태로 조회 */
    public Map<String, Double> findUserRatings(Long userId, String contentType) {
        List<Rated> list = ratedRepo.findAllByUserIdAndContentType(userId, contentType);
        var map = new HashMap<String,Double>();
        for (var r : list) {
            map.put(r.getContentId(), r.getRating());
        }
        return map;
    }
}
