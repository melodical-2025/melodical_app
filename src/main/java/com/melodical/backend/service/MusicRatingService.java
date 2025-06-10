package com.melodical.backend.service;

import com.melodical.backend.dto.*;
import com.melodical.backend.entity.Rated;
import com.melodical.backend.repository.RatedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MusicRatingService {

    private final RatedRepository ratedRepo;

    /** 사용자 + 곡별 평점을 저장(또는 업데이트) */
    public void saveRatings(MusicRatingRequest req) {
        String contentType = req.getType();  // "music" 등
        for (RatingDto r : req.getMusicRatings()) {
            String contentId = r.getContentId();
            double rating = r.getRating();

            Rated record = ratedRepo
                    .findByUserIdAndContentTypeAndContentId(req.getUserId(), contentType, contentId)
                    .orElseGet(() -> Rated.builder()
                            .userId(req.getUserId())
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