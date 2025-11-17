package com.melodical.backend.service.cache;

import com.melodical.backend.dto.CandidateItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 추천 시스템을 위한 Redis 캐싱 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String RECOMMENDATION_PREFIX = "rec:user:";
    private static final String RECENT_VIEWED_PREFIX = "recent:user:";
    private static final String EXPOSURE_FREQUENCY_PREFIX = "exposure:user:";
    private static final long CACHE_TTL_SECONDS = 3600; // 1 hour
    private static final long RECENT_VIEWED_TTL_SECONDS = 604800; // 7 days
    private static final long EXPOSURE_TTL_SECONDS = 2592000; // 30 days

    /**
     * 추천 결과 캐싱
     */
    public void cacheRecommendations(Long userId, String surface, List<CandidateItem> recommendations) {
        String key = RECOMMENDATION_PREFIX + userId + ":" + surface;
        try {
            redisTemplate.opsForValue().set(key, recommendations, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            log.info("Cached recommendations for user {} on surface {}", userId, surface);
        } catch (Exception e) {
            log.error("Failed to cache recommendations", e);
        }
    }

    /**
     * 캐시된 추천 결과 조회
     */
    @SuppressWarnings("unchecked")
    public List<CandidateItem> getCachedRecommendations(Long userId, String surface) {
        String key = RECOMMENDATION_PREFIX + userId + ":" + surface;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.info("Retrieved cached recommendations for user {} on surface {}", userId, surface);
                return (List<CandidateItem>) cached;
            }
        } catch (Exception e) {
            log.error("Failed to retrieve cached recommendations", e);
        }
        return null;
    }

    /**
     * 최근 본 아이템 추가
     */
    public void addRecentlyViewed(Long userId, Long musicalId) {
        String key = RECENT_VIEWED_PREFIX + userId;
        try {
            // Sorted Set을 사용하여 시간순으로 저장 (score는 timestamp)
            redisTemplate.opsForZSet().add(key, musicalId.toString(), System.currentTimeMillis());
            // 최대 100개까지만 유지
            redisTemplate.opsForZSet().removeRange(key, 0, -101);
            redisTemplate.expire(key, RECENT_VIEWED_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("Added musical {} to recently viewed for user {}", musicalId, userId);
        } catch (Exception e) {
            log.error("Failed to add recently viewed item", e);
        }
    }

    /**
     * 최근 본 아이템 조회 (최신순)
     */
    public Set<String> getRecentlyViewed(Long userId, int limit) {
        String key = RECENT_VIEWED_PREFIX + userId;
        try {
            // 최신 limit개 조회 (높은 score부터)
            Set<Object> result = redisTemplate.opsForZSet().reverseRange(key, 0, limit - 1);
            if (result != null) {
                return result.stream()
                        .map(Object::toString)
                        .collect(Collectors.toSet());
            }
        } catch (Exception e) {
            log.error("Failed to get recently viewed items", e);
        }
        return Set.of();
    }

    /**
     * 특정 아이템 노출 횟수 증가
     */
    public void incrementExposureCount(Long userId, Long musicalId) {
        String key = EXPOSURE_FREQUENCY_PREFIX + userId + ":" + musicalId;
        try {
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, EXPOSURE_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("Incremented exposure count for user {} and musical {}", userId, musicalId);
        } catch (Exception e) {
            log.error("Failed to increment exposure count", e);
        }
    }

    /**
     * 특정 아이템 노출 횟수 조회
     */
    public int getExposureCount(Long userId, Long musicalId) {
        String key = EXPOSURE_FREQUENCY_PREFIX + userId + ":" + musicalId;
        try {
            Object count = redisTemplate.opsForValue().get(key);
            return count != null ? Integer.parseInt(count.toString()) : 0;
        } catch (Exception e) {
            log.error("Failed to get exposure count", e);
            return 0;
        }
    }

    /**
     * 캐시 무효화
     */
    public void invalidateCache(Long userId) {
        try {
            String pattern = RECOMMENDATION_PREFIX + userId + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Invalidated cache for user {}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to invalidate cache", e);
        }
    }
}

