package com.melodical.backend.service;

import com.melodical.backend.entity.Musical;
import com.melodical.backend.repository.MusicalRepository;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class MusicalService {

    private final MusicalRepository musicalRepository;

    private final String API_KEY = "fc4376d5310049c5b9290f9859701cb9";

    public void fetchAndSaveMusicals() {
        String url = "http://www.kopis.or.kr/openApi/restful/pblprfr?service=" + API_KEY
                + "&stdate=20250609&eddate=20250731&cpage=1&rows=10&shcate=GGGA&prfstate=02";

        RestTemplate restTemplate = new RestTemplate();
        String xml = restTemplate.getForObject(url, String.class);

        try {
            System.out.println("📦 1. KOPIS 응답 XML:");
            System.out.println(xml);

            JSONObject jsonObj = XML.toJSONObject(xml);

            System.out.println("📦 2. 변환된 JSON:");
            System.out.println(jsonObj.toString(2));

            Object dbsRaw = jsonObj.get("dbs");

            // 🔐 예외 처리 추가: dbs가 JSONObject인지 확인
            if (!(dbsRaw instanceof JSONObject)) {
                System.out.println("⚠ 공연 정보가 없습니다. (빈 응답)");
                return;
            }

            JSONObject dbsObject = (JSONObject) dbsRaw;
            if (dbsObject.isEmpty() || !dbsObject.has("db")) {
                System.out.println("⚠ 공연 정보가 없습니다.");
                return;
            }

            JSONArray shows = dbsObject.optJSONArray("db");
            if (shows == null) {
                // 단일 공연 정보일 경우
                JSONObject singleShow = dbsObject.getJSONObject("db");
                shows = new JSONArray();
                shows.put(singleShow);
            }

            System.out.println("📦 3. 가져온 공연 수: " + shows.length());

            for (int i = 0; i < shows.length(); i++) {
                JSONObject show = shows.getJSONObject(i);

                String title = show.optString("prfnm");
                String theater = show.optString("fcltynm");
                String startDate = show.optString("prfpdfrom");
                String endDate = show.optString("prfpdto");
                String cast = show.optString("prfcast");
                String runtime = show.optString("prfruntime");
                String posterUrl = show.optString("poster");

                System.out.printf("🎭 [%d번째 공연]%n", i + 1);
                System.out.println(" - 제목: " + title);
                System.out.println(" - 장소: " + theater);
                System.out.println(" - 시작일: " + startDate);
                System.out.println(" - 종료일: " + endDate);
                System.out.println(" - 출연진: " + cast);
                System.out.println(" - 런타임: " + runtime);
                System.out.println(" - 포스터: " + posterUrl);

                // DB에 저장
                Musical musical = new Musical();
                musical.setTitle(title);
                musical.setTheater(theater);
                musical.setStartDate(startDate);
                musical.setEndDate(endDate);
                musical.setCast(cast);
                musical.setRuntime(runtime);
                musical.setPosterUrl(posterUrl);

                musicalRepository.save(musical);
            }

            System.out.println("✅ DB 저장 완료");

        } catch (Exception e) {
            System.out.println("❌ 예외 발생:");
            e.printStackTrace();
        }
    }
}
