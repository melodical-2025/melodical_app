package com.melodical.backend.controller;

import com.melodical.backend.dto.ChangePasswordRequest;
import com.melodical.backend.dto.UpdateNicknameRequest;
import com.melodical.backend.dto.UserProfileResponse;
import com.melodical.backend.service.UserService;
import com.melodical.backend.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 프로필 관리 컨트롤러
 * 인증 관련 기능은 AuthController와 SocialAuthController에서 처리합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

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
