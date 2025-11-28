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
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> body) {
        try {
            // Flutter에서 'idToken' 또는 'token' 키로 보낼 수 있음
            String idToken = body.get("idToken");
            if (idToken == null || idToken.isEmpty()) {
                idToken = body.get("token");
            }
            
            if (idToken == null || idToken.isEmpty()) {
                log.error("Google login failed: Missing token. Body keys: {}", body.keySet());
                return ResponseEntity.badRequest().build();
            }

            log.info("Google login request received");
            AuthResponse response = socialAuthService.authenticateWithGoogle(idToken);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Google login failed", e);
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Google login failed", 
                                "message", e.getMessage()));
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
    public ResponseEntity<?> kakaoLogin(@RequestBody Map<String, String> body) {
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
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Kakao login failed", 
                                "message", e.getMessage()));
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
    public ResponseEntity<?> naverLogin(@RequestBody Map<String, String> body) {
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
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Naver login failed", 
                                "message", e.getMessage()));
        }
    }

    /**
     * Apple 로그인
     * Flutter에서 Apple Sign-In으로 받은 Identity Token을 검증하고 JWT 발급
     * 
     * @param body { "identityToken": "Apple Identity Token", "authorizationCode": "..." }
     * @return JWT 토큰 및 사용자 정보
     */
    @PostMapping("/apple")
    public ResponseEntity<?> appleLogin(@RequestBody Map<String, String> body) {
        try {
            String identityToken = body.get("identityToken");
            
            // Flutter에서 'token' 키로 보낼 수도 있으므로 둘 다 체크
            if (identityToken == null || identityToken.isEmpty()) {
                identityToken = body.get("token");
            }
            
            if (identityToken == null || identityToken.isEmpty()) {
                log.error("Apple login failed: Missing token. Body keys: {}", body.keySet());
                return ResponseEntity.badRequest().build();
            }

            log.info("Apple login request received");
            AuthResponse response = socialAuthService.authenticateWithApple(identityToken);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Apple login failed", e);
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Apple login failed", 
                                "message", e.getMessage()));
        }
    }
}
