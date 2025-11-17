package com.melodical.backend.controller;

import com.melodical.backend.service.model.PCTRModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 추천 모델 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/model")
@RequiredArgsConstructor
@Slf4j
public class ModelController {

    private final PCTRModelService pctrModelService;

    /**
     * 모델 정보 조회
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getModelInfo() {
        log.info("Fetching model info");
        Map<String, Object> info = pctrModelService.getModelInfo();
        return ResponseEntity.ok(info);
    }

    /**
     * 특정 피처 가중치 조회
     */
    @GetMapping("/weight/{featureName}")
    public ResponseEntity<Map<String, Object>> getFeatureWeight(@PathVariable String featureName) {
        log.info("Fetching weight for feature: {}", featureName);
        // 실제로는 PCTRModelService에 getWeight 메서드 추가 필요
        return ResponseEntity.ok(Map.of(
                "feature", featureName,
                "message", "Feature weight API - implementation needed"
        ));
    }

    /**
     * 가중치 업데이트 (온라인 학습용)
     */
    @PutMapping("/weight/{featureName}")
    public ResponseEntity<Map<String, String>> updateWeight(
            @PathVariable String featureName,
            @RequestParam double weight) {
        log.info("Updating weight for feature {}: {}", featureName, weight);
        pctrModelService.updateWeight(featureName, weight);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Weight updated for feature: " + featureName
        ));
    }

    /**
     * 모델 재로딩
     */
    @PostMapping("/reload")
    public ResponseEntity<Map<String, String>> reloadModel() {
        log.info("Reloading model");
        try {
            pctrModelService.loadModel();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Model reloaded successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to reload model", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
}

