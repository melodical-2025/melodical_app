package com.melodical.backend.controller;

import com.melodical.backend.dto.AuthResponse;
import com.melodical.backend.service.SocialAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 소셜 로그인 처리를 위한 컨트롤러
 * Flutter에서 직접 받은 소셜 로그인 토큰을 처리
 */
@RestController
@RequestMapping("/auth/oauth2")
@RequiredArgsConstructor
@Slf4j
public class SocialAuthController {

    private final SocialAuthService socialAuthService;

    /**
     * Google 로그인
     * Flutter에서 Google Sign-In으로 받은 ID Token을 검증하고 JWT 발급
     * 
     * @param body { "token": "Google ID Token" }
     * @return JWT 토큰 및 사용자 정보
     */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody Map<String, String> body) {
        try {
            String idToken = body.get("token");
            
            if (idToken == null || idToken.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            log.info("Google login request received");
            AuthResponse response = socialAuthService.authenticateWithGoogle(idToken);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Google login failed", e);
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * Kakao 로그인
     * Flutter에서 Kakao SDK로 받은 Access Token으로 사용자 정보 조회 및 JWT 발급
     * 
     * @param body { "accessToken": "Kakao Access Token" }
     * @return JWT 토큰 및 사용자 정보
     */
    @PostMapping("/kakao")
    public ResponseEntity<AuthResponse> kakaoLogin(@RequestBody Map<String, String> body) {
        try {
            String accessToken = body.get("accessToken");
            
            // Flutter에서 'token' 키로 보낼 수도 있으므로 둘 다 체크
            if (accessToken == null || accessToken.isEmpty()) {
                accessToken = body.get("token");
            }
            
            if (accessToken == null || accessToken.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            log.info("Kakao login request received");
            AuthResponse response = socialAuthService.authenticateWithKakao(accessToken);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Kakao login failed", e);
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * Naver 로그인
     * Flutter에서 Naver SDK로 받은 Access Token으로 사용자 정보 조회 및 JWT 발급
     * 
     * @param body { "accessToken": "Naver Access Token" }
     * @return JWT 토큰 및 사용자 정보
     */
    @PostMapping("/naver")
    public ResponseEntity<AuthResponse> naverLogin(@RequestBody Map<String, String> body) {
        try {
            String accessToken = body.get("accessToken");
            
            // Flutter에서 'token' 키로 보낼 수도 있으므로 둘 다 체크
            if (accessToken == null || accessToken.isEmpty()) {
                accessToken = body.get("token");
            }
            
            if (accessToken == null || accessToken.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            log.info("Naver login request received");
            AuthResponse response = socialAuthService.authenticateWithNaver(accessToken);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Naver login failed", e);
            return ResponseEntity.status(401).build();
        }
    }
}
