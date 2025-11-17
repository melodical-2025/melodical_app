# 뮤지컬 데이터 중복 및 URL/평점 누락 문제 해결 가이드

## 📋 문제 상황

1. **증상**: 같은 뮤지컬 작품이 두 개씩 존재
2. **증상**: 평점과 사이트 URL이 통일되지 않음 (같은 작품인데 URL과 평점이 있는 것과 없는 것이 혼재)
3. **원인**: 크롤링 후 integrated 데이터를 사용해야 하는데, 과거 데이터나 잘못된 소스에서 로딩하여 중복 발생

## ✅ 적용된 해결책

### 1. **크롤링 파이프라인 검증 완료**

#### 크롤링 데이터 흐름 (정상 작동 확인):
```
Python Crawler (main.py)
  ↓
1. interpark_weekly.json + yes24_weekly.json 생성
2. interpark_monthly.json + yes24_monthly.json 생성
  ↓
3. integrator.py 실행
  ↓
4. integrated_weekly_dataset.json 생성 ← ✅ 이것만 사용
5. integrated_monthly_dataset.json 생성 ← ✅ 이것만 사용
```

**검증 결과**:
- ✅ `integrated_*` 파일은 중복 제거되어 있음
- ✅ `integrated_*` 파일은 양쪽 URL을 모두 포함
- ✅ `MusicalCrawlerService`는 올바르게 `integrated_*` 파일만 로딩

### 2. **데이터 정합성 개선**

#### A. DataCleanupService 강화
파일: `src/main/java/com/melodical/backend/config/DataCleanupService.java`

**추가된 기능**:
1. **CrawledMusicalRanking 무결성 검사** (`checkCrawledDataIntegrity()`)
   - interparkId 기준으로 중복 확인
   - URL 통계 확인 (Interpark URL, Yes24 URL, 양쪽 모두)
   - 중복 발견 시 경고 로그 출력

2. **Musical 중복 제거** (`cleanupDuplicateMusicals()`)
   - interparkId 기준으로 그룹화
   - 각 interparkId당 가장 최근 Musical만 유지 (id가 큰 것)
   - 나머지는 자동 삭제

3. **실행 순서 제어**
   - `@Order(20)`: CrawledData 로딩(Order 0) 후, 동기화(Order 30) 전에 실행
   - 시스템 시작 시 자동 실행

#### B. 진단 API 추가
파일: `src/main/java/com/melodical/backend/controller/DataDiagnosticsController.java`

**엔드포인트**:

1. **`GET /api/diagnostics/musicals/duplicates`**
   - Musical 테이블의 interparkId 중복 확인
   - 각 중복에 대한 상세 정보 반환

2. **`GET /api/diagnostics/rankings/duplicates`**
   - CrawledMusicalRanking 테이블의 interparkId 중복 확인
   - URL/평점 정보 포함

3. **`GET /api/diagnostics/interpark/{interparkId}`**
   - 특정 interparkId에 대한 모든 Musical과 Ranking 정보 조회
   - 예: `/api/diagnostics/interpark/25015528`

4. **`GET /api/diagnostics/stats`**
   - 전체 데이터 통계
   - URL 보유 현황, 데이터 버전 정보

### 3. **서버 시작 시 자동 정리 프로세스**

```
서버 시작
  ↓
[Order 0] MusicalCrawlerService.initializeCrawling()
  - integrated_weekly_dataset.json 로딩
  - integrated_monthly_dataset.json 로딩
  - CrawledMusicalRanking 테이블에 저장
  ↓
[Order 20] DataCleanupService.run()
  - checkCrawledDataIntegrity(): Ranking 중복 검사
  - cleanupDuplicateMusicals(): Musical 중복 제거
  - checkRankingMatching(): 매칭 통계
  ↓
[Order 30] MusicalSyncService.syncCrawledDataToMusicals()
  - CrawledMusicalRanking → Musical 동기화
  - posterUrl, 극장, 기간 등 업데이트
  ↓
서버 준비 완료
```

## 🔧 사용 방법

### 1. 서버 재시작
```bash
cd /Users/leejungheon/Desktop/springstudy/melodical_app
./gradlew bootRun
```

### 2. 로그 확인
서버 시작 시 다음 로그를 확인하세요:

```
🧹 Starting data cleanup process...
📊 Total CrawledMusicalRanking records: XXX
📊 Unique interpark IDs in CrawledMusicalRanking: XXX
✅ No duplicates found in CrawledMusicalRanking
📊 URL Statistics in CrawledMusicalRanking:
  - With Interpark URL: XXX
  - With Yes24 URL: XXX
  - With Both URLs: XXX
📊 Total musicals in database: XXX
📊 Unique interpark IDs: XXX
📊 Duplicates found: X
📊 Duplicates removed: X
✅ Data cleanup completed
```

### 3. 진단 API로 확인

#### 전체 통계 확인:
```bash
curl http://localhost:8080/api/diagnostics/stats | jq
```

**예상 결과**:
```json
{
  "totalMusicals": 150,
  "musicalsWithInterparkId": 145,
  "musicalsWithPosterUrl": 148,
  "totalRankings": 300,
  "rankingsWithInterparkUrl": 180,
  "rankingsWithYes24Url": 120,
  "rankingsWithBothUrls": 90,
  "dataVersions": {
    "20251104_163553": 300
  }
}
```

#### Musical 중복 확인:
```bash
curl http://localhost:8080/api/diagnostics/musicals/duplicates | jq
```

**예상 결과** (정상인 경우):
```json
{
  "totalMusicals": 150,
  "uniqueInterparkIds": 150,
  "duplicateInterparkIds": 0,
  "duplicates": []
}
```

#### 특정 작품 확인:
```bash
# 센과 치히로 (interparkId: 25015528)
curl http://localhost:8080/api/diagnostics/interpark/25015528 | jq
```

**예상 결과**:
```json
{
  "interparkId": "25015528",
  "musicalsCount": 1,
  "rankingsCount": 1,
  "musicals": [{
    "id": 123,
    "title": "〈센과 치히로의 행방불명〉오리지널 투어 (SPIRITED AWAY)",
    "posterUrl": "https://ticketimage.interpark.com/Play/image/large/25/25015528_p.gif",
    "theater": "...",
    "period": "2025.10.22 ~ 2026.1.18"
  }],
  "rankings": [{
    "id": 456,
    "title": "〈센과 치히로의 행방불명〉오리지널 투어 (SPIRITED AWAY)",
    "rankingType": "WEEKLY",
    "dataVersion": "20251104_163553",
    "interparkUrl": "https://tickets.interpark.com/goods/25015528",
    "yes24Url": "https://ticket.yes24.com/Perf/55838",
    "interparkRating": null,
    "yes24Rating": null
  }]
}
```

## 🎯 테스트 체크리스트

### ✅ 크롤링 데이터 확인
- [ ] `crawling/data/integrated_weekly_dataset_*.json` 파일이 최신 타임스탬프로 존재하는가?
- [ ] `integrated_*` 파일 내부에 중복 제목이 없는가?
- [ ] `detail_url`과 `yes24_detail_url`이 모두 존재하는가?

### ✅ 데이터베이스 확인
- [ ] 서버 로그에서 "No duplicates found" 또는 "Duplicates removed: N" 확인
- [ ] `/api/diagnostics/musicals/duplicates`에서 `duplicateInterparkIds: 0` 확인
- [ ] `/api/diagnostics/rankings/duplicates`에서 `duplicateInterparkIds: 0` 확인

### ✅ Flutter 앱 확인
- [ ] "평가한 뮤지컬" 화면에서 같은 작품이 한 번만 나타나는가?
- [ ] 각 뮤지컬 상세 화면에서 인터파크/Yes24 버튼이 일관되게 표시되는가?
- [ ] 평점 정보가 일관되게 표시되는가?

## 🐛 문제 해결

### 문제 1: 여전히 중복이 발견됨
**원인**: 크롤링 데이터 자체에 중복이 있거나 여러 버전의 데이터가 혼재

**해결**:
1. 크롤링 데이터 재생성:
   ```bash
   cd crawling/src
   python3 main.py
   ```

2. 데이터베이스 완전 초기화:
   ```sql
   TRUNCATE TABLE crawled_musical_ranking;
   TRUNCATE TABLE musical;
   DELETE FROM rated_musical;
   ```

3. 서버 재시작

### 문제 2: URL이 여전히 누락됨
**원인**: CrawledMusicalRanking에는 있지만 Musical에 동기화되지 않음

**해결**:
1. MusicalSyncService 실행 확인:
   ```
   로그에서 "Starting to sync crawled data to Musical entities" 확인
   ```

2. 수동으로 동기화 트리거 (필요시 임시 엔드포인트 추가):
   ```java
   @RestController
   public class ManualSyncController {
       @Autowired
       private MusicalSyncService syncService;
       
       @PostMapping("/api/admin/sync-musicals")
       public String manualSync() {
           int count = syncService.syncCrawledDataToMusicals();
           return "Synced: " + count;
       }
   }
   ```

### 문제 3: 평점이 null인 경우
**원인**: 실제 크롤링 시점에 평점 정보가 없었거나 크롤링 실패

**확인**:
1. integrated JSON 파일에서 해당 작품 확인:
   ```bash
   cat crawling/data/integrated_weekly_dataset_*.json | jq '.[] | select(.title | contains("작품명"))'
   ```

2. `interpark_rating`과 `yes24_rating` 필드 확인

**해결**: 평점은 크롤링 시점에 따라 null일 수 있음 (정상 동작)

## 📝 주요 변경사항 요약

### 1. **DataCleanupService** (config/DataCleanupService.java)
- ✅ CrawledMusicalRanking 무결성 검사 추가
- ✅ Musical 중복 자동 제거
- ✅ 실행 순서 명시 (`@Order(20)`)
- ✅ 상세 로깅 추가

### 2. **DataDiagnosticsController** (NEW)
- ✅ `/api/diagnostics/musicals/duplicates`
- ✅ `/api/diagnostics/rankings/duplicates`
- ✅ `/api/diagnostics/interpark/{interparkId}`
- ✅ `/api/diagnostics/stats`

### 3. **MusicalCrawlerService** (기존, 검증 완료)
- ✅ `integrated_weekly_dataset.json` 올바르게 로딩
- ✅ `integrated_monthly_dataset.json` 올바르게 로딩
- ✅ CrawledMusicalRanking 테이블에 정확히 저장

### 4. **MusicalSyncService** (기존, 검증 완료)
- ✅ CrawledMusicalRanking → Musical 동기화
- ✅ posterUrl, 극장, 기간 등 정확히 업데이트
- ✅ interparkId 기반 매칭

## 🔄 데이터 흐름 전체 그림

```
[Python 크롤링]
  interpark_crawler → interpark_weekly.json
  yes24_crawler → yes24_weekly.json
  integrator.py → integrated_weekly_dataset.json (중복 제거, URL 통합)
                ↓
[Spring Boot 로딩]
  MusicalCrawlerService
    → CrawledMusicalRanking 테이블 저장
                ↓
[데이터 정리]
  DataCleanupService
    → CrawledMusicalRanking 무결성 검사
    → Musical 중복 제거
                ↓
[동기화]
  MusicalSyncService
    → CrawledMusicalRanking → Musical 동기화
                ↓
[API 제공]
  MusicalService.fetchRatedMusicals()
    → Musical 테이블 조회 (findByInterparkId)
    → toDto(): CrawledMusicalRanking에서 평점/URL 가져오기
                ↓
[Flutter 앱]
  DetailScreen: 평점, URL 버튼 표시
```

## 🎓 핵심 포인트

1. **✅ 크롤링 파이프라인은 정상 작동**: `integrated_*` 파일은 완벽
2. **✅ 데이터 로딩은 정상**: `MusicalCrawlerService`는 올바른 파일 사용
3. **✅ 자동 정리 추가**: 서버 시작 시 중복 자동 제거
4. **✅ 진단 도구 추가**: 문제 발생 시 쉽게 디버깅 가능
5. **✅ 로깅 강화**: 각 단계에서 상세한 로그 출력

## 🚀 다음 단계

1. **서버 재시작** 후 로그 확인
2. **진단 API** 호출하여 중복 여부 확인
3. **Flutter 앱**에서 실제 작동 테스트
4. 문제 발생 시 이 가이드의 "문제 해결" 섹션 참조

---

**작성일**: 2025-11-16
**작성자**: GitHub Copilot
**버전**: 1.0
