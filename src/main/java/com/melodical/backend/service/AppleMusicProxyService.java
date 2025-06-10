package com.melodical.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class AppleMusicProxyService {
    private final AppleMusicTokenService tokenService;
    private final RestTemplate rt = new RestTemplate();

    /**
     * 한국 차트의 Top Songs 를 가져와 그대로 반환
     */
    public String fetchKoreanTopSongsCharts() throws Exception {
        String devToken = tokenService.generateDeveloperToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(devToken);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        // Apple Music Charts API (KR, songs 차트)
        ResponseEntity<String> resp = rt.exchange(
                "https://api.music.apple.com/v1/catalog/kr/charts?types=songs",
                HttpMethod.GET,
                req,
                String.class
        );
        return resp.getBody();
    }
}