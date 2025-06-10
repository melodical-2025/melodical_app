package com.melodical.backend.controller;

import com.melodical.backend.service.MusicalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/musicals")
public class MusicalController {

    private final MusicalService musicalService;

    @GetMapping("/fetch")
    public String fetchAndSaveMusicals() {
        System.out.println("🎯 fetchAndSaveMusicals 진입"); // ✅ 로깅 추가
        musicalService.fetchAndSaveMusicals();
        return "뮤지컬 데이터 저장 완료!";
    }
}
