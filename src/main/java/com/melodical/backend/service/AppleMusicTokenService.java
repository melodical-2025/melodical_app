package com.melodical.backend.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AppleMusicTokenService {

    @Value("${apple.music.team-id}")
    private String teamId;

    @Value("${apple.music.key-id}")
    private String keyId;

    @Value("${apple.music.private-key-location}")
    private Resource privateKeyResource;

    @Value("${apple.music.token-validity-seconds}")
    private Long tokenValiditySeconds;

    /**
     * Apple Music 개발자 토큰(JWT) 생성
     */
    public String generateDeveloperToken() {
        try (InputStream is = privateKeyResource.getInputStream()) {
            // .p8 파일 내용 전체 읽어서 불필요한 헤더/개행 제거
            String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] der = Base64.getDecoder().decode(pem);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            KeyFactory kf = KeyFactory.getInstance("EC");
            PrivateKey privateKey = kf.generatePrivate(spec);

            Date now = new Date();
            Date exp = new Date(now.getTime() + tokenValiditySeconds * 1000);

            // JWT 헤더에 alg=ES256, kid=<KEY_ID> 추가, claim 으로 iss(teamId), iat, exp 설정
            return Jwts.builder()
                    .setHeaderParam("alg", "ES256")
                    .setHeaderParam("kid", keyId)
                    .setIssuer(teamId)
                    .setIssuedAt(now)
                    .setExpiration(exp)
                    .signWith(privateKey, SignatureAlgorithm.ES256)
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Apple Music developer token", e);
        }
    }
}