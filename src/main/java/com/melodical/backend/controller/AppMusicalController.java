package com.melodical.backend.controller;

import com.melodical.backend.service.MusicalDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/app/musicals")
@RequiredArgsConstructor
public class AppMusicalController {
    private final MusicalDataService musicalDataService;

    /**
     * 월간 인기 뮤지컬 목록
     */
    @GetMapping("/monthly")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyMusicals() {
        try {
            List<Map<String, Object>> musicals = musicalDataService.getMonthlyMusicals();
            return ResponseEntity.ok(musicals);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 주간 인기 뮤지컬 목록
     */
    @GetMapping("/weekly")
    public ResponseEntity<List<Map<String, Object>>> getWeeklyMusicals() {
        try {
            List<Map<String, Object>> musicals = musicalDataService.getWeeklyMusicals();
            return ResponseEntity.ok(musicals);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 월간 인기 Top N
     */
    @GetMapping("/monthly/top/{count}")
    public ResponseEntity<List<Map<String, Object>>> getTopMonthlyMusicals(
            @PathVariable int count) {
        try {
            List<Map<String, Object>> musicals = musicalDataService.getTopMonthlyMusicals(count);
            return ResponseEntity.ok(musicals);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 통합 검색 (KOPIS + Crawled Data)
     */
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchMusicals(
            @RequestParam String query,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<Map<String, Object>> results = musicalDataService.searchMusicals(query, limit);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 뮤지컬 상세 조회 (ID로 조회)
     * 검색 화면과 평가 페이지에서 상세 페이지로 이동할 때 사용
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMusicalDetail(@PathVariable Long id) {
        try {
            Map<String, Object> musical = musicalDataService.getMusicalDetail(id);
            if (musical == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(musical);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 뮤지컬 상세 조회 (interparkId로 조회)
     */
    @GetMapping("/interpark/{interparkId}")
    public ResponseEntity<Map<String, Object>> getMusicalByInterparkId(@PathVariable String interparkId) {
        try {
            Map<String, Object> musical = musicalDataService.getMusicalByInterparkId(interparkId);
            if (musical == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(musical);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
