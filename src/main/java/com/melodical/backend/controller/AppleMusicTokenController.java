package com.melodical.backend.controller;

import com.melodical.backend.service.AppleMusicTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/token")
@RequiredArgsConstructor
public class AppleMusicTokenController {

    private final AppleMusicTokenService tokenService;

    @GetMapping("/applemusic")
    public ResponseEntity<Map<String, String>> developerToken() throws Exception {
        String token = tokenService.generateDeveloperToken();
        return ResponseEntity.ok(Map.of("token", token));
    }
}