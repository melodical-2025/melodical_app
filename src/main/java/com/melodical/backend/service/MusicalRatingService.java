package com.melodical.backend.service;

import com.melodical.backend.dto.MusicalRatingDto;
import com.melodical.backend.dto.MusicalRatingRequest;
import com.melodical.backend.entity.Musical;
import com.melodical.backend.entity.RatedMusical;
import com.melodical.backend.entity.User;
import com.melodical.backend.repository.MusicalRepository;
import com.melodical.backend.repository.RatedMusicalRepository;
import com.melodical.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MusicalRatingService {
    private final RatedMusicalRepository ratedMusicalRepo;
    private final UserRepository userRepo;
    private final MusicalRepository musicalRepo;

    public void saveRatings(MusicalRatingRequest req) {
        Long userId = req.getUserId();
        User userEntity = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid userId: " + userId));

        for (MusicalRatingDto dto : req.getMusicalRatings()) {
            Long musicalId = Long.valueOf(dto.getMusicalId());
            Musical musicalEntity = musicalRepo.findById(musicalId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid musicalId: " + musicalId));

            Double score = dto.getRating();

            RatedMusical record = ratedMusicalRepo
                    .findByUserAndMusical(userEntity, musicalEntity)
                    .orElseGet(() -> RatedMusical.builder()
                            .user(userEntity)
                            .musical(musicalEntity)
                            .build());

            record.setRating(score);
            record.setRatedAt(LocalDateTime.now());
            ratedMusicalRepo.save(record);
        }
    }

    /**
     * userId 가 평가한 뮤지컬을 Map<musicalId, rating> 으로 조회
     */
    public Map<Long, Double> findUserRatings(Long userId) {
        List<RatedMusical> list = ratedMusicalRepo.findByUserId(userId);

        Map<Long, Double> map = new HashMap<>();
        for (RatedMusical r : list) {
            map.put(r.getMusical().getId(), r.getRating());
        }
        return map;
    }
}