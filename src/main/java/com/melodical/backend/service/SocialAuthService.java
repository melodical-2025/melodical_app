package com.melodical.backend.service;

import com.melodical.backend.dto.AuthResponse;
import com.melodical.backend.entity.User;
import com.melodical.backend.repository.UserRepository;
import com.melodical.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Optional;

/**
 * 소셜 로그인 처리를 위한 서비스
 * Google, Kakao, Naver의 토큰을 검증하고 사용자 정보를 조회
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SocialAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    /**
     * Google ID Token 검증 및 사용자 생성/로그인
     * @param idToken Google ID Token
     * @return JWT 토큰이 포함된 인증 응답
     */
    @Transactional
    public AuthResponse authenticateWithGoogle(String idToken) {
        try {
            // Google ID Token에서 페이로드 추출 (간단한 검증)
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid Google ID Token format");
            }

            // Base64 디코딩
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode jsonNode = objectMapper.readTree(payload);

            // 사용자 정보 추출
            String email = jsonNode.get("email").asText();
            String name = jsonNode.has("name") ? jsonNode.get("name").asText() : email.split("@")[0];
            String googleId = jsonNode.get("sub").asText();

            log.info("Google login attempt - email: {}, googleId: {}", email, googleId);

            // 사용자 조회 또는 생성
            User user = findOrCreateUser(email, name, "GOOGLE", googleId);

            // JWT 토큰 생성
            String jwtToken = jwtService.generateToken(user.getEmail(), user.getId());

            return AuthResponse.builder()
                    .token(jwtToken)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .build();

        } catch (Exception e) {
            log.error("Google authentication failed", e);
            throw new RuntimeException("Google 인증 실패: " + e.getMessage());
        }
    }

    /**
     * Kakao Access Token으로 사용자 정보 조회 및 로그인
     * @param accessToken Kakao Access Token
     * @return JWT 토큰이 포함된 인증 응답
     */
    @Transactional
    public AuthResponse authenticateWithKakao(String accessToken) {
        try {
            // Kakao API로 사용자 정보 조회
            WebClient webClient = webClientBuilder.build();
            String response = webClient.get()
                    .uri("https://kapi.kakao.com/v2/user/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode jsonNode = objectMapper.readTree(response);

            // 사용자 정보 추출
            String kakaoId = jsonNode.get("id").asText();
            JsonNode kakaoAccount = jsonNode.get("kakao_account");
            String email = kakaoAccount.has("email") ? kakaoAccount.get("email").asText() : null;
            String nickname = kakaoAccount.has("profile") && kakaoAccount.get("profile").has("nickname")
                    ? kakaoAccount.get("profile").get("nickname").asText()
                    : "Kakao_" + kakaoId;

            // 이메일이 없으면 kakaoId로 임시 이메일 생성
            if (email == null || email.isEmpty()) {
                email = "kakao_" + kakaoId + "@melodical.temp";
            }

            log.info("Kakao login attempt - email: {}, kakaoId: {}", email, kakaoId);

            // 사용자 조회 또는 생성
            User user = findOrCreateUser(email, nickname, "KAKAO", kakaoId);

            // JWT 토큰 생성
            String jwtToken = jwtService.generateToken(user.getEmail(), user.getId());

            return AuthResponse.builder()
                    .token(jwtToken)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .build();

        } catch (Exception e) {
            log.error("Kakao authentication failed", e);
            throw new RuntimeException("Kakao 인증 실패: " + e.getMessage());
        }
    }

    /**
     * Naver Access Token으로 사용자 정보 조회 및 로그인
     * @param accessToken Naver Access Token
     * @return JWT 토큰이 포함된 인증 응답
     */
    @Transactional
    public AuthResponse authenticateWithNaver(String accessToken) {
        try {
            // Naver API로 사용자 정보 조회
            WebClient webClient = webClientBuilder.build();
            String response = webClient.get()
                    .uri("https://openapi.naver.com/v1/nid/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode jsonNode = objectMapper.readTree(response);

            // 응답 확인
            if (!"00".equals(jsonNode.get("resultcode").asText())) {
                throw new RuntimeException("Naver API 호출 실패");
            }

            JsonNode responseNode = jsonNode.get("response");

            // 사용자 정보 추출
            String naverId = responseNode.get("id").asText();
            String email = responseNode.has("email") ? responseNode.get("email").asText() : null;
            String name = responseNode.has("name") ? responseNode.get("name").asText() : null;
            String nickname = responseNode.has("nickname") ? responseNode.get("nickname").asText() : name;

            // 이메일이 없으면 naverId로 임시 이메일 생성
            if (email == null || email.isEmpty()) {
                email = "naver_" + naverId + "@melodical.temp";
            }

            // 닉네임이 없으면 기본값 설정
            if (nickname == null || nickname.isEmpty()) {
                nickname = "Naver_" + naverId;
            }

            log.info("Naver login attempt - email: {}, naverId: {}", email, naverId);

            // 사용자 조회 또는 생성
            User user = findOrCreateUser(email, nickname, "NAVER", naverId);

            // JWT 토큰 생성
            String jwtToken = jwtService.generateToken(user.getEmail(), user.getId());

            return AuthResponse.builder()
                    .token(jwtToken)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .build();

        } catch (Exception e) {
            log.error("Naver authentication failed", e);
            throw new RuntimeException("Naver 인증 실패: " + e.getMessage());
        }
    }

    /**
     * 사용자 조회 또는 생성
     * 이미 존재하는 사용자면 조회, 없으면 새로 생성
     */
    private User findOrCreateUser(String email, String nickname, String provider, String providerId) {
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            log.info("Existing user found: {}", email);
            
            // 소셜 로그인 정보 업데이트 (필요한 경우)
            if (user.getProvider() == null || user.getProvider().isEmpty()) {
                user.setProvider(provider);
                user.setProviderId(providerId);
                userRepository.save(user);
            }
            
            return user;
        }

        // 새 사용자 생성
        User newUser = User.builder()
                .email(email)
                .name(nickname)
                .provider(provider)
                .providerId(providerId)
                .password(null)  // 소셜 로그인은 비밀번호 없음
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("New user created: {}, provider: {}", email, provider);

        return savedUser;
    }
}
