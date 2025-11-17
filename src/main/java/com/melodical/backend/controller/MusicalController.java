package com.melodical.backend.controller;

import com.melodical.backend.dto.MusicalRatingRequest;
import com.melodical.backend.dto.MusicalResponseDto;
import com.melodical.backend.security.UserPrincipal;
import com.melodical.backend.service.MusicalRatingService;
import com.melodical.backend.service.MusicalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
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
    public ResponseEntity<?> rateMusical(
            @Valid @RequestBody MusicalRatingRequest req) {
        try {
            musicalRatingService.saveRatings(req);
            // 성공 시 바디 없이 204 응답
            return ResponseEntity.ok(Map.of("message", "저장 완료"));
        } catch (IllegalArgumentException ex) {
            // 잘못된 요청 데이터
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            // 서버 에러
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "서버 에러가 발생했습니다"));
        }
    }
    @PostMapping("/rate/batch")
    public ResponseEntity<?> rateBatchMusical(
            @Valid @RequestBody Map<String, Object> payload){
        try {
            log.info("📥 Received batch rating request: {}", payload);
            
            // JWT 토큰에서 userId 추출
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                log.error("❌ Unauthenticated request");
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "인증되지 않은 사용자입니다"));
            }
            
            // UserPrincipal에서 userId 추출
            Long userId;
            try {
                Object principal = authentication.getPrincipal();
                log.info("🔐 Principal type: {}, value: {}", principal.getClass().getName(), principal);
                
                if (principal instanceof UserPrincipal) {
                    UserPrincipal userPrincipal = (UserPrincipal) principal;
                    userId = userPrincipal.getUser().getId();
                    log.info("✅ Extracted userId from UserPrincipal: {}", userId);
                } else {
                    log.error("❌ Principal is not UserPrincipal: {}", principal.getClass().getName());
                    return ResponseEntity
                            .status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("error", "잘못된 인증 정보입니다"));
                }
            } catch (Exception e) {
                log.error("❌ Failed to extract userId", e);
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "사용자 정보를 추출할 수 없습니다"));
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ratings =
                    (List<Map<String, Object>>) payload.get("ratings");
            if(ratings == null || ratings.isEmpty()){
                log.warn("⚠️ Empty ratings list");
                return ResponseEntity
                        .badRequest()
                        .body(Map.of("error", "평점 데이터가 없습니다"));
            }

            log.info("📊 Processing {} ratings", ratings.size());
            
            // 각 rating에 userId 추가
            for (Map<String, Object> rating : ratings) {
                rating.put("userId", userId);
                log.debug("Rating: {}", rating);
            }

            musicalRatingService.saveBatchRatings(ratings);
            log.info("✅ Successfully saved {} ratings", ratings.size());
            
            return ResponseEntity.ok(Map.of("message", "일괄 저장 완료"));
        }catch (Exception ex){
            log.error("❌ Error saving batch ratings", ex);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }
    
    /**
     * 사용자가 평가한 뮤지컬 ID와 평점 목록 조회
     */
    @GetMapping("/ratings/my")
    public ResponseEntity<?> getMyRatings() {
        try {
            // JWT 토큰에서 userId 추출
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "인증되지 않은 사용자입니다"));
            }
            
            Long userId;
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal) {
                userId = ((UserPrincipal) principal).getUser().getId();
            } else {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "잘못된 인증 정보입니다"));
            }
            
            Map<Long, Double> myRatings = musicalRatingService.getMyRatings(userId);
            log.info("✅ Retrieved {} ratings for user {}", myRatings.size(), userId);
            
            return ResponseEntity.ok(myRatings);
        } catch (Exception ex) {
            log.error("❌ Error fetching user ratings", ex);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }
}
