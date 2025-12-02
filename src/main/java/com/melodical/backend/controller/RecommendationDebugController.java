package com.melodical.backend.controller;

import com.melodical.backend.dto.RecommendationRequest;
import com.melodical.backend.dto.RecommendationResponse;
import com.melodical.backend.service.recommendation.RecommendationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 추천 시스템 디버깅 및 분석용 컨트롤러
 * 단계별 추천 결과를 JSON 파일로 추출
 */
@Slf4j
@RestController
@RequestMapping("/api/recommendations/debug")
@RequiredArgsConstructor
public class RecommendationDebugController {

    private final RecommendationService recommendationService;
    private final ObjectMapper objectMapper;

    /**
     * 추천 결과를 JSON 파일로 저장
     * GET /api/recommendations/debug/export?userId=1&outputDir=/path/to/output
     * 
     * @param userId 사용자 ID
     * @param outputDir 출력 디렉토리 (선택, 기본값: ./recommendation_exports)
     * @param count 추천 개수 (선택, 기본값: 20)
     * @return 저장된 파일 경로들
     */
    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> exportRecommendations(
            @RequestParam Long userId,
            @RequestParam(required = false) String outputDir,
            @RequestParam(defaultValue = "20") Integer count) {
        
        try {
            log.info("🔍 Starting recommendation export for userId={}", userId);
            
            // 출력 디렉토리 설정
            String baseDir = outputDir != null ? outputDir : "./recommendation_exports";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String exportDir = baseDir + "/user_" + userId + "_" + timestamp;
            
            // 디렉토리 생성
            Path exportPath = Paths.get(exportDir);
            Files.createDirectories(exportPath);
            log.info("📁 Export directory created: {}", exportPath.toAbsolutePath());
            
            // ObjectMapper 설정 (예쁘게 출력)
            ObjectMapper prettyMapper = objectMapper.copy();
            prettyMapper.enable(SerializationFeature.INDENT_OUTPUT);
            
            // 추천 생성
            log.info("📊 Generating recommendations...");
            RecommendationRequest request = RecommendationRequest.builder()
                    .userId(userId)
                    .count(count)
                    .build();
            List<RecommendationResponse> recommendations = recommendationService.recommend(request);
            
            // JSON 파일로 저장
            String recommendationsFile = exportDir + "/recommendations.json";
            prettyMapper.writeValue(new File(recommendationsFile), 
                    createRecommendationsReport(recommendations, userId, count));
            log.info("✅ Recommendations exported: {} items → {}", recommendations.size(), recommendationsFile);
            
            // 요약 정보 생성
            Map<String, Object> summary = new HashMap<>();
            summary.put("userId", userId);
            summary.put("timestamp", timestamp);
            summary.put("exportDirectory", exportPath.toAbsolutePath().toString());
            summary.put("files", Map.of(
                "recommendations", recommendationsFile
            ));
            summary.put("count", recommendations.size());
            
            String summaryFile = exportDir + "/summary.json";
            prettyMapper.writeValue(new File(summaryFile), summary);
            log.info("✅ Summary exported: {}", summaryFile);
            
            return ResponseEntity.ok(summary);
            
        } catch (IOException e) {
            log.error("❌ Failed to export recommendations", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "파일 저장 실패: " + e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Unexpected error during export", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "추천 생성 실패: " + e.getMessage()));
        }
    }

    /**
     * 추천 결과를 JSON으로 반환 (파일 저장 없이)
     * GET /api/recommendations/debug/json?userId=1&count=20
     */
    @GetMapping("/json")
    public ResponseEntity<Map<String, Object>> getRecommendationsJson(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "20") Integer count) {
        
        try {
            log.info("🔍 Getting recommendations for userId={}", userId);
            
            // 추천 생성
            RecommendationRequest request = RecommendationRequest.builder()
                    .userId(userId)
                    .count(count)
                    .build();
            List<RecommendationResponse> recommendations = recommendationService.recommend(request);
            
            // 결과 조합
            Map<String, Object> result = createRecommendationsReport(recommendations, userId, count);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to get recommendations", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 추천 리포트 생성
     */
    private Map<String, Object> createRecommendationsReport(List<RecommendationResponse> recommendations, 
                                                             Long userId, Integer requestedCount) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("userId", userId);
        report.put("timestamp", LocalDateTime.now().toString());
        report.put("requestedCount", requestedCount);
        report.put("actualCount", recommendations.size());
        
        // 소스별 개수 집계
        Map<String, Long> sourceCount = recommendations.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getSource() != null ? r.getSource() : "unknown",
                        Collectors.counting()
                ));
        report.put("recommendationsBySource", sourceCount);
        
        // 최종 점수 통계
        OptionalDouble avgFinalScore = recommendations.stream()
                .mapToDouble(r -> r.getFinalScore() != null ? r.getFinalScore() : 0.0)
                .average();
        OptionalDouble maxFinalScore = recommendations.stream()
                .mapToDouble(r -> r.getFinalScore() != null ? r.getFinalScore() : 0.0)
                .max();
        
        report.put("scoreStats", Map.of(
            "averageFinalScore", avgFinalScore.orElse(0.0),
            "maxFinalScore", maxFinalScore.orElse(0.0)
        ));
        
        // 최종 추천 리스트
        List<Map<String, Object>> recommendationList = new ArrayList<>();
        int position = 1;
        for (RecommendationResponse rec : recommendations) {
            Map<String, Object> recMap = new LinkedHashMap<>();
            recMap.put("position", position++);
            recMap.put("musicalId", rec.getMusicalId());
            recMap.put("title", rec.getTitle());
            recMap.put("posterUrl", rec.getPosterUrl());
            recMap.put("theater", rec.getTheater());
            recMap.put("genre", rec.getGenre());
            recMap.put("source", rec.getSource());
            recMap.put("finalScore", rec.getFinalScore());
            recMap.put("stage1Score", rec.getStage1Score());
            recMap.put("pctrScore", rec.getPctrScore());
            recMap.put("contentCrossScore", rec.getContentCrossScore());
            recMap.put("cfCrossScore", rec.getCfCrossScore());
            recMap.put("chartRanking", rec.getChartRanking());
            recMap.put("recommendationReason", rec.getRecommendationReason());
            recMap.put("similarityPercentage", rec.getSimilarityPercentage());
            recMap.put("averageRating", rec.getAverageRating());
            recMap.put("interparkUrl", rec.getInterparkUrl());
            recMap.put("yes24Url", rec.getYes24Url());
            recommendationList.add(recMap);
        }
        report.put("recommendations", recommendationList);
        
        return report;
    }

    /**
     * 특정 파일 다운로드
     * GET /api/recommendations/debug/download?file=/path/to/file.json
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadFile(@RequestParam String file) {
        try {
            Path filePath = Paths.get(file);
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            byte[] content = Files.readAllBytes(filePath);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", filePath.getFileName().toString());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
                    
        } catch (Exception e) {
            log.error("Failed to download file: {}", file, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 음악 취향 기반 추천만 조회 (JSON 자동 저장)
     * GET /api/recommendations/debug/music-taste?userId=1&count=10
     */
    @GetMapping("/music-taste")
    public ResponseEntity<Map<String, Object>> getMusicTasteRecommendations(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") Integer count,
            @RequestParam(defaultValue = "true") Boolean saveToFile) {
        
        try {
            log.info("🎵 Getting music taste recommendations for userId={}", userId);
            
            // 추천 생성 - 충분한 수를 가져오기 위해
            RecommendationRequest request = RecommendationRequest.builder()
                    .userId(userId)
                    .count(Math.max(count * 2, 50))
                    .build();
            List<RecommendationResponse> allRecommendations = recommendationService.recommend(request);
            
            // 음악 취향 기반만 필터링 및 finalScore 기준 내림차순 정렬
            List<RecommendationResponse> musicTasteRecommendations = allRecommendations.stream()
                    .filter(rec -> "music_taste_only".equals(rec.getSource()))
                    .sorted((a, b) -> Double.compare(
                            b.getFinalScore() != null ? b.getFinalScore() : 0.0,
                            a.getFinalScore() != null ? a.getFinalScore() : 0.0
                    ))
                    .limit(count)
                    .collect(Collectors.toList());
            
            // 결과 생성
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("userId", userId);
            result.put("timestamp", LocalDateTime.now().toString());
            result.put("type", "music_taste_only");
            result.put("count", musicTasteRecommendations.size());
            result.put("recommendations", createRecommendationList(musicTasteRecommendations));
            
            // JSON 파일로 저장
            if (saveToFile) {
                String filePath = saveToJsonFile(result, userId, "music_taste");
                result.put("savedFile", filePath);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to get music taste recommendations", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 뮤지컬 취향 기반 추천만 조회 (JSON 자동 저장)
     * GET /api/recommendations/debug/musical-taste?userId=1&count=10
     */
    @GetMapping("/musical-taste")
    public ResponseEntity<Map<String, Object>> getMusicalTasteRecommendations(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") Integer count,
            @RequestParam(defaultValue = "true") Boolean saveToFile) {
        
        try {
            log.info("🎭 Getting musical taste recommendations for userId={}", userId);
            
            // 추천 생성 - 충분한 수를 가져오기 위해
            RecommendationRequest request = RecommendationRequest.builder()
                    .userId(userId)
                    .count(Math.max(count * 2, 50))
                    .build();
            List<RecommendationResponse> allRecommendations = recommendationService.recommend(request);
            
            // 뮤지컬 취향 기반만 필터링 (combined_algorithm만 사용) 및 finalScore 기준 내림차순 정렬
            List<RecommendationResponse> musicalTasteRecommendations = allRecommendations.stream()
                    .filter(rec -> "combined_algorithm".equals(rec.getSource()))
                    .sorted((a, b) -> Double.compare(
                            b.getFinalScore() != null ? b.getFinalScore() : 0.0,
                            a.getFinalScore() != null ? a.getFinalScore() : 0.0
                    ))
                    .limit(count)
                    .collect(Collectors.toList());
            
            // 결과 생성
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("userId", userId);
            result.put("timestamp", LocalDateTime.now().toString());
            result.put("type", "musical_taste_based");
            result.put("count", musicalTasteRecommendations.size());
            result.put("recommendations", createRecommendationList(musicalTasteRecommendations));
            
            // JSON 파일로 저장
            if (saveToFile) {
                String filePath = saveToJsonFile(result, userId, "musical_taste");
                result.put("savedFile", filePath);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to get musical taste recommendations", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 인기차트 기반 추천만 조회 (JSON 자동 저장)
     * GET /api/recommendations/debug/popularity?userId=1&count=10
     */
    @GetMapping("/popularity")
    public ResponseEntity<Map<String, Object>> getPopularityRecommendations(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") Integer count,
            @RequestParam(defaultValue = "true") Boolean saveToFile) {
        
        try {
            log.info("⭐ Getting popularity chart recommendations for userId={}", userId);
            
            // 추천 생성 - 충분한 수를 가져오기 위해
            RecommendationRequest request = RecommendationRequest.builder()
                    .userId(userId)
                    .count(Math.max(count * 2, 50))
                    .build();
            List<RecommendationResponse> allRecommendations = recommendationService.recommend(request);
            
            // 인기차트 기반만 필터링 및 finalScore 기준 내림차순 정렬
            List<RecommendationResponse> popularityRecommendations = allRecommendations.stream()
                    .filter(rec -> "popularity_only".equals(rec.getSource()))
                    .sorted((a, b) -> Double.compare(
                            b.getFinalScore() != null ? b.getFinalScore() : 0.0,
                            a.getFinalScore() != null ? a.getFinalScore() : 0.0
                    ))
                    .limit(count)
                    .collect(Collectors.toList());
            
            // 결과 생성
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("userId", userId);
            result.put("timestamp", LocalDateTime.now().toString());
            result.put("type", "popularity_chart");
            result.put("count", popularityRecommendations.size());
            result.put("recommendations", createRecommendationList(popularityRecommendations));
            
            // JSON 파일로 저장
            if (saveToFile) {
                String filePath = saveToJsonFile(result, userId, "popularity");
                result.put("savedFile", filePath);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to get popularity recommendations", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 모든 단계별 추천 한번에 조회 (JSON 자동 저장)
     * GET /api/recommendations/debug/all-stages?userId=1&count=10
     */
    @GetMapping("/all-stages")
    public ResponseEntity<Map<String, Object>> getAllStages(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") Integer count,
            @RequestParam(defaultValue = "true") Boolean saveToFile) {
        
        try {
            log.info("📊 Getting all stage recommendations for userId={}", userId);
            
            // 추천 생성 - 충분한 수를 가져오기 위해 100개로 설정
            RecommendationRequest request = RecommendationRequest.builder()
                    .userId(userId)
                    .count(100)
                    .build();
            List<RecommendationResponse> allRecommendations = recommendationService.recommend(request);
            
            // 각 단계별로 분류 및 finalScore 기준 내림차순 정렬
            List<RecommendationResponse> musicTaste = allRecommendations.stream()
                    .filter(rec -> "music_taste_only".equals(rec.getSource()))
                    .sorted((a, b) -> Double.compare(
                            b.getFinalScore() != null ? b.getFinalScore() : 0.0,
                            a.getFinalScore() != null ? a.getFinalScore() : 0.0
                    ))
                    .limit(count)
                    .collect(Collectors.toList());
            
            List<RecommendationResponse> musicalTaste = allRecommendations.stream()
                    .filter(rec -> "combined_algorithm".equals(rec.getSource()))
                    .sorted((a, b) -> Double.compare(
                            b.getFinalScore() != null ? b.getFinalScore() : 0.0,
                            a.getFinalScore() != null ? a.getFinalScore() : 0.0
                    ))
                    .limit(count)
                    .collect(Collectors.toList());
            
            List<RecommendationResponse> popularity = allRecommendations.stream()
                    .filter(rec -> "popularity_only".equals(rec.getSource()))
                    .sorted((a, b) -> Double.compare(
                            b.getFinalScore() != null ? b.getFinalScore() : 0.0,
                            a.getFinalScore() != null ? a.getFinalScore() : 0.0
                    ))
                    .limit(count)
                    .collect(Collectors.toList());
            
            // 결과 생성
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("userId", userId);
            result.put("timestamp", LocalDateTime.now().toString());
            result.put("musicTasteRecommendations", Map.of(
                    "count", musicTaste.size(),
                    "recommendations", createRecommendationList(musicTaste)
            ));
            result.put("musicalTasteRecommendations", Map.of(
                    "count", musicalTaste.size(),
                    "recommendations", createRecommendationList(musicalTaste)
            ));
            result.put("popularityRecommendations", Map.of(
                    "count", popularity.size(),
                    "recommendations", createRecommendationList(popularity)
            ));
            
            // JSON 파일로 저장
            if (saveToFile) {
                String filePath = saveToJsonFile(result, userId, "all_stages");
                result.put("savedFile", filePath);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to get all stage recommendations", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 추천 리스트를 JSON 친화적인 형태로 변환
     */
    private List<Map<String, Object>> createRecommendationList(List<RecommendationResponse> recommendations) {
        List<Map<String, Object>> list = new ArrayList<>();
        int position = 1;
        
        for (RecommendationResponse rec : recommendations) {
            Map<String, Object> recMap = new LinkedHashMap<>();
            recMap.put("position", position++);
            recMap.put("musicalId", rec.getMusicalId());
            recMap.put("title", rec.getTitle());
            recMap.put("posterUrl", rec.getPosterUrl());
            recMap.put("theater", rec.getTheater());
            recMap.put("genre", rec.getGenre());
            recMap.put("source", rec.getSource());
            recMap.put("finalScore", rec.getFinalScore());
            recMap.put("chartRanking", rec.getChartRanking());
            recMap.put("recommendationReason", rec.getRecommendationReason());
            recMap.put("similarityPercentage", rec.getSimilarityPercentage());
            recMap.put("averageRating", rec.getAverageRating());
            recMap.put("interparkUrl", rec.getInterparkUrl());
            recMap.put("yes24Url", rec.getYes24Url());
            list.add(recMap);
        }
        
        return list;
    }
    
    /**
     * JSON 파일로 저장
     */
    private String saveToJsonFile(Map<String, Object> data, Long userId, String type) throws IOException {
        String baseDir = "./recommendation_exports";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String exportDir = baseDir + "/user_" + userId + "_" + timestamp;
        
        // 디렉토리 생성
        Path exportPath = Paths.get(exportDir);
        Files.createDirectories(exportPath);
        
        // JSON 파일 저장
        String fileName = type + "_recommendations.json";
        String filePath = exportDir + "/" + fileName;
        
        ObjectMapper prettyMapper = objectMapper.copy();
        prettyMapper.enable(SerializationFeature.INDENT_OUTPUT);
        prettyMapper.writeValue(new File(filePath), data);
        
        log.info("✅ Saved {} recommendations to: {}", type, filePath);
        
        return filePath;
    }
}
