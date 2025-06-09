package com.melodical.backend.loader;

import com.melodical.backend.service.MusicalService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MusicalLoader implements CommandLineRunner {

    private final MusicalService musicalService;

    @Override
    public void run(String... args) {
        System.out.println("🚀 서버 실행 시 자동으로 fetchAndSaveMusicals 호출 시작");
        musicalService.fetchAndSaveMusicals();
    }
}
