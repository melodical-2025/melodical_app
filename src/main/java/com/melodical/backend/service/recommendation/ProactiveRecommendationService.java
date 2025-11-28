package com.melodical.backend.service.recommendation;

import com.melodical.backend.dto.CandidateItem;
import com.melodical.backend.dto.RecommendationRequest;
import com.melodical.backend.entity.User;
import com.melodical.backend.repository.UserRepository;
import com.melodical.backend.service.cache.RecommendationCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 사전 추천 생성 서비스
 * - 서버 시작 시 모든 사용자의 추천 목록을 미리 생성
 * - 사용자 평가 발생 시 해당 사용자의 추천 목록만 재생성
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProactiveRecommendationService {

    private final UserRepository userRepository;
    private final RecommendationCacheService cacheService;
    private final Stage1CandidateService stage1Service;
    private final Stage2PCTRRankingService stage2Service;
    private final Stage3TwiddlerService stage3Service;
    private final RatingBasedSimilarityService ratingBasedSimilarityService;

    private static final int DEFAULT_CANDIDATE_COUNT = 300;
    
    /**
     * 서버 시작 시 모든 사용자의 추천 생성
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void preGenerateAllRecommendations() {
        log.info("🚀 Starting proactive recommendation generation for all users...");
        long startTime = System.currentTimeMillis();
        
        try {
            List<User> allUsers = userRepository.findAll();
            log.info("📊 Found {} users to generate recommendations", allUsers.size());
            
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);
            
            for (User user : allUsers) {
                try {
                    // 각 사용자별로 home, search, profile 표면의 추천 생성
                    generateRecommendationsForUser(user.getId(), "home");
                    successCount.incrementAndGet();
                    
                    if (successCount.get() % 10 == 0) {
                        log.info("⏳ Progress: {}/{} users processed", successCount.get(), allUsers.size());
                    }
                } catch (Exception e) {
                    log.error("❌ Failed to generate recommendations for user {}: {}", 
                            user.getId(), e.getMessage());
                    failCount.incrementAndGet();
                }
            }
            
            long endTime = System.currentTimeMillis();
            log.info("✅ Proactive recommendation generation completed: {} succeeded, {} failed, took {}ms",
                    successCount.get(), failCount.get(), endTime - startTime);
        } catch (Exception e) {
            log.error("❌ Proactive recommendation generation failed", e);
        }
    }
    
    /**
     * 특정 사용자의 추천 생성 (평가 발생 시 호출)
     */
    @Async
    @Transactional(readOnly = true)
    public void generateRecommendationsForUser(Long userId, String surface) {
        log.info("🔄 Generating recommendations for user: {}, surface: {}", userId, surface);
        
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            
            // Stage-1: 후보 생성
            List<CandidateItem> candidates = stage1Service.generateCandidates(
                    user, "KR", DEFAULT_CANDIDATE_COUNT);
            
            if (candidates.isEmpty()) {
                log.warn("⚠️ No candidates generated for user: {}", userId);
                return;
            }
            
            // Stage-2: pCTR 랭킹
            RecommendationRequest mockRequest = new RecommendationRequest();
            mockRequest.setUserId(userId);
            mockRequest.setSurface(surface);
            mockRequest.setRegion("KR");
            mockRequest.setCount(20);
            
            List<CandidateItem> rankedCandidates = stage2Service.rankCandidatesByPCTR(
                    user, candidates, mockRequest);
            
            // 사용자가 평가한 작품 목록 가져오기
            Set<String> userRatedMusicals = ratingBasedSimilarityService.getUserRatedMusicalIds(userId);
            
            // Stage-3: Twiddler 후처리
            List<CandidateItem> finalCandidates = stage3Service.applyTwiddlerPolicies(
                    user, rankedCandidates, mockRequest, userRatedMusicals);
            
            // 캐시에 저장 (30분 TTL)
            cacheService.cacheRecommendations(userId, surface, finalCandidates);
            
            log.info("✅ Successfully generated {} recommendations for user: {}", 
                    finalCandidates.size(), userId);
        } catch (Exception e) {
            log.error("❌ Failed to generate recommendations for user: {}", userId, e);
        }
    }
    
    /**
     * 추천 재생성 (캐시 무효화 및 새 추천 생성)
     */
    @Async
    public void refreshRecommendations(Long userId) {
        log.info("🔄 Refreshing recommendations for user: {}", userId);
        
        // 1. 캐시 무효화
        cacheService.invalidateCache(userId);
        log.info("🗑️ Cache invalidated for user: {}", userId);
        
        // 2. 새 추천 생성 (자동으로 캐시 업데이트됨)
        generateRecommendationsForUser(userId, "home");
        
        log.info("✅ Recommendations refreshed for user: {}", userId);
    }
}
