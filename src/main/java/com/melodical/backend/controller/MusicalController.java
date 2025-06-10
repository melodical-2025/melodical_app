package com.melodical.backend.controller;

import com.melodical.backend.dto.MusicalRatingRequest;
import com.melodical.backend.dto.MusicalResponseDto;
import com.melodical.backend.service.MusicalRatingService;
import com.melodical.backend.service.MusicalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/musicals")
@RequiredArgsConstructor
public class MusicalController {
    private final MusicalService service;
    private final MusicalRatingService musicalRatingService;

    /** 전체 뮤지컬 목록 조회 */
    @GetMapping("/fetch")
    public ResponseEntity<List<MusicalResponseDto>> fetchAll() {
        List<MusicalResponseDto> list = service.fetchAllMusicals();
        return ResponseEntity.ok(list);
    }
    @GetMapping("/rated")
    public ResponseEntity<List<MusicalResponseDto>> fetchRated(
        @RequestParam("userId") Long userId){
        return ResponseEntity.ok(service.fetchRatedMusicals(userId));
    }
    @PostMapping("/rate")
    public ResponseEntity<Void> rateMusical(
            @Valid @RequestBody MusicalRatingRequest req
    ) {
        try {
            musicalRatingService.saveRatings(req);
            // 성공 시 바디 없이 204 응답
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            // 잘못된 요청 데이터
            return ResponseEntity
                    .badRequest()
                    .body(null);
        } catch (Exception ex) {
            // 서버 에러
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}
