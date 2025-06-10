package com.melodical.backend.controller;

import com.melodical.backend.dto.MusicRatingRequest;
import com.melodical.backend.dto.SongResponseDto;
import com.melodical.backend.dto.SongDto;
import com.melodical.backend.service.AppleMusicService;
import com.melodical.backend.service.MusicRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/music")
@CrossOrigin(origins = "*")  // 필요에 따라 도메인 제한
@RequiredArgsConstructor
@Validated
public class MusicController {

    private final AppleMusicService appleService;
    private final MusicRatingService ratingService;

    /**
     * 1) Apple Music 차트 조회 + DB에 저장된 사용자의 평점(userRating) 포함해서 반환
     *
     * @param userId JWT나 세션에서 받아오셔도 좋고, 간단히 파라미터로 받습니다.
     */
    @GetMapping("/top")
    public ResponseEntity<List<SongResponseDto>> topSongs(
            @RequestParam("userId") Long userId) {

        // 1) Apple Music 차트 목록
        List<SongDto> songs = appleService.fetchTopSongs();

        // 2) DB에서 이 사용자가 매겨둔 평점 맵(id → rating)
        Map<String, Double> userRatings = ratingService.findUserRatings(userId, "music");

        // 3) 합쳐서 DTO로 변환
        List<SongResponseDto> result = songs.stream()
                .map(song -> {
                    Double rating = userRatings.get(song.getId());
                    return SongResponseDto.builder()
                            .id(song.getId())
                            .title(song.getTitle())
                            .artist(song.getArtist())
                            .artworkUrl(song.getArtworkUrl())
                            .genre(song.getGenre())
                            .userRating(rating)        // 평점이 없으면 null
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * 2) 사용자가 보낸 평점 저장
     *    최초(5점) 또는 재조정 등 모든 평점 변경을 이 한 엔드포인트로 처리합니다.
     */
    @PostMapping("/rate")
    public ResponseEntity<Void> rateMusic(
            @Valid @RequestBody MusicRatingRequest request) {

        ratingService.saveRatings(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/rated")
    public ResponseEntity<List<SongResponseDto>> ratedSongs(
            @RequestParam("userId") Long userId) {

        // 1) DB에서 이 사용자가 매겨둔 평점 맵(id → rating)
        Map<String, Double> userRatings = ratingService.findUserRatings(userId, "music");

        // 2) Apple Music 차트 목록에서 사용자 평가 항목만 필터링
        List<SongDto> allSongs = appleService.fetchTopSongs();
        List<SongResponseDto> result = allSongs.stream()
                .filter(song -> userRatings.containsKey(song.getId()))
                .map(song -> SongResponseDto.builder()
                        .id(song.getId())
                        .title(song.getTitle())
                        .artist(song.getArtist())
                        .artworkUrl(song.getArtworkUrl())
                        .genre(song.getGenre())
                        .userRating(userRatings.get(song.getId()))
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}