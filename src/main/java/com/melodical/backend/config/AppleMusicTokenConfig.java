// src/main/java/com/melodical/backend/config/AppleMusicTokenConfig.java
package com.melodical.backend.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Configuration
@RequiredArgsConstructor
public class AppleMusicTokenConfig {

    private final AppleMusicProperties props;

    @Bean("developerToken")
    public String developerToken() {
        try {
            // 1) classpath:AuthKey_*.p8 에 명시한 위치에서 PEM 읽기
            String location = props.getPrivateKeyLocation().replace("classpath:", "");
            Resource res = new ClassPathResource(location);
            String pem;
            try (InputStream in = res.getInputStream()) {
                pem = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            }

            // 2) 헤더/푸터 제거하고 Base64 디코드
            String base64 = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] der = Base64.getDecoder().decode(base64);

            // 3) PKCS#8 스펙으로 EC PrivateKey 생성
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(der);
            KeyFactory kf = KeyFactory.getInstance("EC");
            PrivateKey privateKey = kf.generatePrivate(keySpec);

            // 4) JWT 빌드 (ES256)
            long now = System.currentTimeMillis();
            return Jwts.builder()
                    .setHeaderParam("kid", props.getKeyId())
                    .setIssuer(props.getTeamId())
                    .setIssuedAt(new Date(now))
                    .setExpiration(new Date(now + props.getTokenValiditySeconds() * 1000))
                    .signWith(privateKey, SignatureAlgorithm.ES256)
                    .compact();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate Apple Music developer token", e);
        }
    }
}