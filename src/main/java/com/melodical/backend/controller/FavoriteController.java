package com.melodical.backend.controller;

import com.melodical.backend.entity.Favorite;
import com.melodical.backend.service.FavoriteService;
import com.melodical.backend.service.MusicalDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final MusicalDataService musicalDataService;

    /**
     * 찜 추가
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addFavorite(
            @RequestBody Map<String, Long> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long musicalId = request.get("musicalId");
        favoriteService.addFavorite(userDetails.getUsername(), musicalId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "찜 추가 완료");
        response.put("isFavorite", true);
        return ResponseEntity.ok(response);
    }

    /**
     * 찜 취소
     */
    @DeleteMapping("/{musicalId}")
    public ResponseEntity<Map<String, Object>> removeFavorite(
            @PathVariable Long musicalId,
            @AuthenticationPrincipal UserDetails userDetails) {
        favoriteService.removeFavorite(userDetails.getUsername(), musicalId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "찜 취소 완료");
        response.put("isFavorite", false);
        return ResponseEntity.ok(response);
    }

    /**
     * 찜 토글 (있으면 삭제, 없으면 추가)
     */
    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggleFavorite(
            @RequestBody Map<String, Long> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long musicalId = request.get("musicalId");
        boolean isFavorite = favoriteService.toggleFavorite(userDetails.getUsername(), musicalId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", isFavorite ? "찜 추가 완료" : "찜 취소 완료");
        response.put("isFavorite", isFavorite);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 찜 목록 조회 (뮤지컬 상세 정보 포함)
     */
    @GetMapping("/my")
    public ResponseEntity<List<Map<String, Object>>> getMyFavorites(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<Favorite> favorites = favoriteService.getFavoritesByUser(userDetails.getUsername());
        
        // Musical 정보를 포함한 DTO로 변환
        List<Map<String, Object>> result = favorites.stream()
                .map(favorite -> {
                    Map<String, Object> data = musicalDataService.convertToMap(favorite.getMusical(), null);
                    data.put("favoritedAt", favorite.getCreatedAt());
                    return data;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 찜 여부 확인
     */
    @GetMapping("/check/{musicalId}")
    public ResponseEntity<Map<String, Boolean>> checkFavorite(
            @PathVariable Long musicalId,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isFavorite = favoriteService.isFavorite(userDetails.getUsername(), musicalId);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("isFavorite", isFavorite);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 찜 개수
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getFavoriteCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        long count = favoriteService.getFavoriteCount(userDetails.getUsername());
        
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }
}
