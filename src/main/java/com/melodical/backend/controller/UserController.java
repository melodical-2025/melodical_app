package com.melodical.backend.controller;

import com.melodical.backend.dto.ChangePasswordRequest;
import com.melodical.backend.dto.UpdateNicknameRequest;
import com.melodical.backend.dto.UserProfileResponse;
import com.melodical.backend.dto.UserStatsResponse;
import com.melodical.backend.service.UserService;
import com.melodical.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 사용자 프로필 관리 컨트롤러
 * 인증 관련 기능은 AuthController와 SocialAuthController에서 처리합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final FileStorageService fileStorageService;

    /**
     * 현재 로그인한 사용자 정보 조회
     * GET /api/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        
        String email = authentication.getName();
        UserProfileResponse profile = userService.getUserProfile(email);
        return ResponseEntity.ok(profile);
    }

    /**
     * 사용자 ID로 프로필 조회
     * GET /api/users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable Long userId) {
        UserProfileResponse profile = userService.getUserProfileById(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * 사용자 통계 정보 조회
     * GET /api/users/{userId}/stats
     */
    @GetMapping("/{userId}/stats")
    public ResponseEntity<UserStatsResponse> getUserStats(@PathVariable Long userId) {
        UserStatsResponse stats = userService.getUserStats(userId);
        return ResponseEntity.ok(stats);
    }
    /**
     * 닉네임 변경
     * PUT /api/users/nickname
     * Body: { "nickname": "새로운닉네임" }
     */
    @PutMapping("/nickname")
    public ResponseEntity<UserProfileResponse> updateNickname(
            Authentication authentication,
            @RequestBody UpdateNicknameRequest request) {
        
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        
        String email = authentication.getName();
        UserProfileResponse updated = userService.updateNickname(email, request.getNickname());
        return ResponseEntity.ok(updated);
    }

    /**
     * 프로필 이미지 업로드
     * POST /api/users/profile-image
     * Content-Type: multipart/form-data
     * Body: file (이미지 파일)
     */
    @PostMapping("/profile-image")
    public ResponseEntity<UserProfileResponse> uploadProfileImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        
        log.info("🔵 프로필 이미지 업로드 요청");
        
        if (authentication == null) {
            log.warn("⚠️ 인증 정보 없음");
            return ResponseEntity.status(401).build();
        }
        
        String email = authentication.getName();
        log.info("사용자 이메일: {}", email);
        log.info("파일명: {}, 크기: {} bytes, Content-Type: {}", 
                 file.getOriginalFilename(), file.getSize(), file.getContentType());
        
        // 파일 유효성 검증
        if (!fileStorageService.isValidImageFile(file)) {
            log.error("❌ 유효하지 않은 이미지 파일");
            return ResponseEntity.badRequest().build();
        }
        
        // 파일 크기 제한 (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            log.error("❌ 파일 크기 초과: {} bytes (최대 5MB)", file.getSize());
            return ResponseEntity.badRequest().build();
        }
        
        try {
            // 파일 저장
            String fileUrl = fileStorageService.storeFile(file);
            log.info("파일 저장 완료: {}", fileUrl);
            
            // 사용자 프로필 이미지 업데이트
            UserProfileResponse updated = userService.updateProfileImage(email, fileUrl);
            log.info("✅ 프로필 이미지 업로드 성공");
            
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("❌ 프로필 이미지 업로드 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 프로필 이미지 삭제
     * DELETE /api/users/profile-image
     */
    @DeleteMapping("/profile-image")
    public ResponseEntity<UserProfileResponse> deleteProfileImage(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        
        String email = authentication.getName();
        UserProfileResponse updated = userService.updateProfileImage(email, null);
        
        return ResponseEntity.ok(updated);
    }

    /**
     * 비밀번호 변경
     * PUT /api/users/password
     * Body: { "currentPassword": "현재비번", "newPassword": "새비번" }
     */
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @RequestBody ChangePasswordRequest request) {
        
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        
        String email = authentication.getName();
        userService.changePassword(email, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    /**
     * 회원 탈퇴
     * DELETE /api/users/me
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        
        String email = authentication.getName();
        userService.deleteUser(email);
        return ResponseEntity.ok().build();
    }
}
