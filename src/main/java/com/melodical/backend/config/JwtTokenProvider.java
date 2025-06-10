package com.melodical.backend.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {
    @Value("${security.jwt.secret-key}")
    private String secret;               // application.properties 에서 읽음

    private final long validityInMs = 3_600_000; // 1시간

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * JWT 생성: subject(email) 외에 userId 클레임 추가
     */
    public String generateToken(String email, Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)                 // ★ userId 클레임 추가
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + validityInMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰 유효성 점검
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 토큰에서 이메일(subject) 꺼내기
     */
    public String getEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 토큰에서 userId 클레임 꺼내기
     */
    public Long getUserId(String token) {
        Claims claims = parseClaims(token);
        Object id = claims.get("userId");
        if (id instanceof Number) {
            return ((Number) id).longValue();
        }
        return Long.parseLong(id.toString());
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}