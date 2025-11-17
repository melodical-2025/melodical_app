package com.melodical.backend.service.model;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * pCTR 모델 로더 및 추론 서비스
 * 로지스틱 회귀 가중치를 파일에서 로딩하여 추론에 사용
 */
@Service
@Slf4j
public class PCTRModelService {

    private Map<String, Double> weights = new HashMap<>();
    private double bias = 0.0;

    @PostConstruct
    public void loadModel() {
        try {
            loadWeightsFromFile();
            log.info("Successfully loaded pCTR model with {} features", weights.size());
        } catch (Exception e) {
            log.warn("Failed to load model weights from file, using default weights", e);
            initializeDefaultWeights();
        }
    }

    /**
     * 파일에서 가중치 로딩
     */
    private void loadWeightsFromFile() throws Exception {
        ClassPathResource resource = new ClassPathResource("model/pctr_weights.txt");

        if (!resource.exists()) {
            throw new RuntimeException("Model weight file not found");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    double value = Double.parseDouble(parts[1].trim());

                    if ("bias".equals(key)) {
                        bias = value;
                    } else {
                        weights.put(key, value);
                    }
                }
            }
        }
    }

    /**
     * 기본 가중치 초기화 (파일 로딩 실패 시)
     */
    private void initializeDefaultWeights() {
        // 사용자 피처 가중치
        weights.put("user_genre_rock", 0.3);
        weights.put("user_genre_pop", 0.25);
        weights.put("user_genre_classical", 0.2);
        weights.put("user_genre_jazz", 0.22);
        weights.put("user_genre_electronic", 0.18);
        weights.put("user_genre_hip-hop", 0.15);
        weights.put("user_genre_r&b", 0.17);

        weights.put("user_recent_clicks", 0.4);
        weights.put("user_profile_strength", 0.3);
        weights.put("user_region_seoul", 0.1);

        // 아이템 피처 가중치
        weights.put("item_is_on_sale", 0.5);
        weights.put("item_is_new", 0.3);
        weights.put("item_popularity", 0.4);
        weights.put("item_region_seoul", 0.15);
        weights.put("item_region_busan", 0.12);
        weights.put("item_price_range", 0.05);

        // 장르 피처
        weights.put("item_genre_로맨스", 0.25);
        weights.put("item_genre_코미디", 0.22);
        weights.put("item_genre_드라마", 0.28);
        weights.put("item_genre_액션", 0.18);
        weights.put("item_genre_판타지", 0.20);
        weights.put("item_genre_호러", 0.10);
        weights.put("item_genre_스릴러", 0.15);

        // 교차 피처 가중치
        weights.put("cross_content_score", 1.2);
        weights.put("cross_cf_score", 1.0);
        weights.put("cross_stage1_score", 0.8);
        weights.put("cross_content_cf_interaction", 0.6);
        weights.put("cross_past_exposure", -0.3); // 이미 본 것은 감점
        weights.put("cross_region_match", 0.25);

        // 컨텍스트 피처 가중치
        weights.put("context_hour_morning", 0.05);
        weights.put("context_hour_afternoon", 0.08);
        weights.put("context_hour_evening", 0.2);
        weights.put("context_hour_night", -0.1);
        weights.put("context_is_weekend", 0.15);
        weights.put("context_surface_home", 0.1);
        weights.put("context_surface_search", 0.08);
        weights.put("context_surface_detail", 0.12);
        weights.put("context_device_mobile", 0.05);

        bias = -1.5; // 기본 바이어스

        log.info("Initialized default weights with {} features", weights.size());
    }

    /**
     * pCTR 예측
     */
    public double predict(Map<String, Double> features) {
        double logit = bias;

        // 가중합 계산
        for (Map.Entry<String, Double> feature : features.entrySet()) {
            String featureName = feature.getKey();
            double featureValue = feature.getValue();
            double weight = weights.getOrDefault(featureName, 0.0);

            logit += weight * featureValue;
        }

        // 시그모이드 함수 적용
        return sigmoid(logit);
    }

    /**
     * 시그모이드 함수
     */
    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    /**
     * 모델 통계 정보
     */
    public Map<String, Object> getModelInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("model_type", "Logistic Regression");
        info.put("feature_count", weights.size());
        info.put("bias", bias);
        info.put("features", weights.keySet());
        return info;
    }

    /**
     * 가중치 업데이트 (온라인 학습을 위한 메서드)
     */
    public void updateWeight(String feature, double weight) {
        weights.put(feature, weight);
        log.debug("Updated weight for feature {}: {}", feature, weight);
    }

    /**
     * 바이어스 업데이트
     */
    public void updateBias(double newBias) {
        this.bias = newBias;
        log.debug("Updated bias: {}", newBias);
    }
}

