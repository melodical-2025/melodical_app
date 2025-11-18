package com.melodical.backend.service.recommendation;

import com.melodical.backend.entity.Rated;
import com.melodical.backend.entity.RatedMusical;
import com.melodical.backend.entity.User;
import com.melodical.backend.repository.RatedMusicalRepository;
import com.melodical.backend.repository.RatedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 평점 기반 사용자 유사도 계산 서비스
 * - 음악 평점 벡터 기반 유사도
 * - 뮤지컬 평점 벡터 기반 유사도
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RatingBasedSimilarityService {

    private final RatedRepository ratedRepository;
    private final RatedMusicalRepository ratedMusicalRepository;

    /**
     * 음악 평점 기반 사용자 벡터 생성
     * Key: contentId (Apple Music Song ID), Value: rating
     */
    public Map<String, Double> buildMusicRatingVector(Long userId) {
        List<Rated> musicRatings = ratedRepository.findByUserIdAndContentType(userId, "music");
        
        Map<String, Double> vector = new HashMap<>();
        for (Rated rated : musicRatings) {
            vector.put(rated.getContentId(), rated.getRating());
        }
        
        log.debug("Built music rating vector for user {}: {} items", userId, vector.size());
        return vector;
    }

    /**
     * 뮤지컬 평점 기반 사용자 벡터 생성
     * Key: musicalId (Interpark ID), Value: rating
     */
    public Map<String, Double> buildMusicalRatingVector(Long userId) {
        List<RatedMusical> musicalRatings = ratedMusicalRepository.findByUserId(userId);
        
        Map<String, Double> vector = new HashMap<>();
        for (RatedMusical rated : musicalRatings) {
            vector.put(rated.getMusicalId(), rated.getRating());
        }
        
        log.debug("Built musical rating vector for user {}: {} items", userId, vector.size());
        return vector;
    }

    /**
     * 코사인 유사도 계산
     * @param vector1 사용자 1의 평점 벡터
     * @param vector2 사용자 2의 평점 벡터
     * @return 코사인 유사도 (0.0 ~ 1.0)
     */
    public double calculateCosineSimilarity(Map<String, Double> vector1, Map<String, Double> vector2) {
        // 공통 아이템 찾기
        Set<String> commonItems = new HashSet<>(vector1.keySet());
        commonItems.retainAll(vector2.keySet());
        
        if (commonItems.isEmpty()) {
            return 0.0;
        }
        
        // 내적 계산
        double dotProduct = 0.0;
        for (String item : commonItems) {
            dotProduct += vector1.get(item) * vector2.get(item);
        }
        
        // 각 벡터의 크기 계산
        double magnitude1 = Math.sqrt(vector1.values().stream()
                .mapToDouble(v -> v * v)
                .sum());
        
        double magnitude2 = Math.sqrt(vector2.values().stream()
                .mapToDouble(v -> v * v)
                .sum());
        
        if (magnitude1 == 0.0 || magnitude2 == 0.0) {
            return 0.0;
        }
        
        double similarity = dotProduct / (magnitude1 * magnitude2);
        return Math.max(0.0, Math.min(1.0, similarity)); // 0~1 범위로 클리핑
    }

    /**
     * 음악 취향이 유사한 사용자 찾기
     * @param targetUserId 대상 사용자
     * @param candidateUserIds 후보 사용자 목록
     * @param topK 상위 K명
     * @return 유사도가 높은 사용자 ID와 유사도 맵 (내림차순)
     */
    public Map<Long, Double> findSimilarUsersByMusicTaste(Long targetUserId, 
                                                            List<Long> candidateUserIds, 
                                                            int topK) {
        Map<String, Double> targetVector = buildMusicRatingVector(targetUserId);
        
        if (targetVector.isEmpty()) {
            log.debug("User {} has no music ratings", targetUserId);
            return Collections.emptyMap();
        }
        
        Map<Long, Double> similarities = new HashMap<>();
        
        for (Long candidateId : candidateUserIds) {
            if (candidateId.equals(targetUserId)) {
                continue; // 자기 자신 제외
            }
            
            Map<String, Double> candidateVector = buildMusicRatingVector(candidateId);
            if (candidateVector.isEmpty()) {
                continue;
            }
            
            double similarity = calculateCosineSimilarity(targetVector, candidateVector);
            if (similarity > 0.0) {
                similarities.put(candidateId, similarity);
            }
        }
        
        // 유사도 내림차순 정렬 후 상위 K개 반환
        return similarities.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * 뮤지컬 취향이 유사한 사용자 찾기
     * @param targetUserId 대상 사용자
     * @param candidateUserIds 후보 사용자 목록
     * @param topK 상위 K명
     * @return 유사도가 높은 사용자 ID와 유사도 맵 (내림차순)
     */
    public Map<Long, Double> findSimilarUsersByMusicalTaste(Long targetUserId, 
                                                              List<Long> candidateUserIds, 
                                                              int topK) {
        Map<String, Double> targetVector = buildMusicalRatingVector(targetUserId);
        
        if (targetVector.isEmpty()) {
            log.debug("User {} has no musical ratings", targetUserId);
            return Collections.emptyMap();
        }
        
        Map<Long, Double> similarities = new HashMap<>();
        
        for (Long candidateId : candidateUserIds) {
            if (candidateId.equals(targetUserId)) {
                continue; // 자기 자신 제외
            }
            
            Map<String, Double> candidateVector = buildMusicalRatingVector(candidateId);
            if (candidateVector.isEmpty()) {
                continue;
            }
            
            double similarity = calculateCosineSimilarity(targetVector, candidateVector);
            if (similarity > 0.0) {
                similarities.put(candidateId, similarity);
            }
        }
        
        // 유사도 내림차순 정렬 후 상위 K개 반환
        return similarities.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * 유사 사용자들이 높게 평가한 뮤지컬 추출
     * @param similarUsers 유사 사용자 ID와 유사도 맵
     * @param excludeMusicalIds 제외할 뮤지컬 ID (이미 본 작품 등)
     * @return 뮤지컬 ID와 가중 평점 맵
     */
    public Map<String, Double> getWeightedMusicalScores(Map<Long, Double> similarUsers, 
                                                          Set<String> excludeMusicalIds) {
        Map<String, Double> weightedScores = new HashMap<>();
        Map<String, Double> totalWeights = new HashMap<>();
        
        for (Map.Entry<Long, Double> entry : similarUsers.entrySet()) {
            Long userId = entry.getKey();
            Double userSimilarity = entry.getValue();
            
            List<RatedMusical> ratings = ratedMusicalRepository.findByUserId(userId);
            
            for (RatedMusical rating : ratings) {
                String musicalId = rating.getMusicalId();
                
                // 제외 목록에 있으면 스킵
                if (excludeMusicalIds.contains(musicalId)) {
                    continue;
                }
                
                // 가중 평점 누적 (유사도 * 평점)
                double weightedRating = userSimilarity * rating.getRating();
                weightedScores.merge(musicalId, weightedRating, Double::sum);
                totalWeights.merge(musicalId, userSimilarity, Double::sum);
            }
        }
        
        // 평균 가중 평점 계산
        Map<String, Double> avgWeightedScores = new HashMap<>();
        for (String musicalId : weightedScores.keySet()) {
            double totalScore = weightedScores.get(musicalId);
            double totalWeight = totalWeights.get(musicalId);
            avgWeightedScores.put(musicalId, totalScore / totalWeight);
        }
        
        return avgWeightedScores;
    }

    /**
     * 사용자가 이미 평가한 뮤지컬 ID 목록 조회
     */
    public Set<String> getUserRatedMusicalIds(Long userId) {
        return ratedMusicalRepository.findByUserId(userId).stream()
                .map(RatedMusical::getMusicalId)
                .collect(Collectors.toSet());
    }
}
