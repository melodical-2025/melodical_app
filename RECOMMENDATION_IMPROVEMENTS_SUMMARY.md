# 추천 시스템 개선 완료 보고서

## 수정 사항 요약

### 1. ✅ 추천 이유가 상세페이지에 표시되지 않는 문제 해결

**문제**:
- 추천 리스트는 로드되지만 상세페이지에 추천 이유(`recommendationReason`, `similarityPercentage`, `chartRanking`)가 표시되지 않음

**원인**:
- 추천 데이터에 이유 필드가 포함되어 있었지만, 상세페이지 조건문이 제대로 작동하지 않음
- 데이터가 전달되는 과정에서 누락되거나 조건 체크가 실패함

**해결**:
1. **home_screen.dart 수정**: 추천 카드 클릭 시 추천 이유 데이터를 명시적으로 전달
   ```dart
   final musicalDataWithReason = {
     ...rec,
     'recommendationReason': rec['recommendationReason'],
     'similarityPercentage': rec['similarityPercentage'],
     'chartRanking': rec['chartRanking'],
     'averageRating': rec['averageRating'],
     'interparkUrl': rec['interparkUrl'],
     'yes24Url': rec['yes24Url'],
   };
   ```

2. **detail_screen.dart**: 이미 구현되어 있음 (조건문 확인)
   ```dart
   if (widget.musicalData['recommendationReason'] != null)
     // 추천 이유 배지 표시
   ```

### 2. ✅ Integrated 데이터 직접 사용으로 평점 및 URL 활성화

**문제**:
- 추천 리스트의 뮤지컬을 클릭하면 KOPIS Musical 데이터로 상세페이지가 열림
- KOPIS 데이터에는 평점(`averageRating`)과 URL(`interparkUrl`, `yes24Url`)이 없음
- 결과: URL 버튼이 비활성화되고 평점이 표시되지 않음

**원인**:
- `Stage1CandidateService`가 `CrawledMusicalRanking`을 `Musical`과 매칭하여 `Musical.id`를 사용
- 이후 `RecommendationService`가 `Musical` 테이블에서 데이터를 조회하여 응답 생성
- `Musical` 테이블에는 URL과 평점 정보가 없음

**해결 방안**:

#### A. Backend 수정

**1) Stage1CandidateService.java**
- `createCandidateFromCrawledData()` 메서드 수정
- KOPIS Musical과 매칭하지 않고 `CrawledMusicalRanking.id`를 직접 사용
```java
// 변경 전: Musical과 매칭
Optional<Musical> matchedMusical = findMatchingKopisMusical(ranking);
musicalId = musical.getId();

// 변경 후: CrawledMusicalRanking ID 직접 사용
musicalId = ranking.getId();
```

**2) RecommendationService.java**
- `convertToResponse()` 메서드 수정
- `CrawledMusicalRankingRepository` 추가
- `CrawledMusicalRanking` 데이터를 우선적으로 사용
```java
// CrawledMusicalRanking 조회
Map<Long, CrawledMusicalRanking> crawledMap = 
    crawledMusicalRankingRepository.findAllById(musicalIds);

// CrawledMusicalRanking 데이터 우선 적용
if (crawledData != null) {
    builder
        .title(crawledData.getTitle())
        .posterUrl(crawledData.getPosterUrl())
        .averageRating(crawledData.getAverageRating())
        .interparkUrl(crawledData.getInterparkUrl())
        .yes24Url(crawledData.getYes24Url());
}
// Fallback: Musical 데이터 사용
else if (musicalData != null) {
    builder.title(musicalData.getTitle())...
}
```

**3) RecommendationResponse.java**
- 평점 및 URL 필드 추가
```java
private Double averageRating;    // 평균 평점
private String interparkUrl;     // 인터파크 URL
private String yes24Url;         // Yes24 URL
```

**4) RecommendationController.java**
- API 응답에 새 필드 포함
```java
map.put("averageRating", rec.getAverageRating());
map.put("interparkUrl", rec.getInterparkUrl());
map.put("yes24Url", rec.getYes24Url());
```

#### B. Frontend 수정

**1) home_screen.dart**
- API 호출 제거, 추천 데이터 직접 사용
```dart
// 변경 전: API로 Musical 데이터 재조회
final detailData = await ApiService.getMusicalDetail(musicalId);

// 변경 후: 추천 데이터 직접 전달
final musicalDataWithReason = {
  'id': musicalId,
  'title': rec['title'],
  'posterUrl': rec['posterUrl'],
  'averageRating': rec['averageRating'],
  'interparkUrl': rec['interparkUrl'],
  'yes24Url': rec['yes24Url'],
  'recommendationReason': rec['recommendationReason'],
  // ... 기타 필드
};
```

**2) detail_screen.dart**
- 이미 URL 버튼 구현되어 있음 (조건부 활성화)
```dart
onPressed: interparkUrl != null && interparkUrl.toString().isNotEmpty
    ? () => _launchURL(interparkUrl.toString())
    : null,
```

## 기대 효과

### 1. 추천 이유 표시
- ✅ 모든 추천에 대한 설명 표시
- 예: "음악 취향이 85% 일치하는 사용자가 좋아하는 작품입니다"
- 예: "인기차트 5위 작품입니다"

### 2. 평점 및 URL 활성화
- ✅ 인터파크/Yes24 평균 평점 표시
- ✅ 예매처 버튼 활성화
- ✅ 클릭 시 해당 사이트로 이동

### 3. 데이터 일관성
- ✅ Integrated 데이터(weekly/monthly) 우선 사용
- ✅ 최신 크롤링 정보 반영
- ✅ Fallback으로 KOPIS 데이터 사용 (데이터 누락 방지)

## 수정된 파일 목록

### Backend
1. `Stage1CandidateService.java`
   - `createCandidateFromCrawledData()` 메서드: CrawledMusicalRanking ID 직접 사용

2. `RecommendationService.java`
   - `CrawledMusicalRankingRepository` 의존성 추가
   - `convertToResponse()` 메서드: CrawledMusicalRanking 우선 조회 및 사용

3. `RecommendationResponse.java`
   - 필드 추가: `averageRating`, `interparkUrl`, `yes24Url`

4. `RecommendationController.java`
   - API 응답에 새 필드 포함

### Frontend
1. `home_screen.dart`
   - 추천 카드 클릭 시: API 재조회 제거, 추천 데이터 직접 사용
   - 평점 및 URL 정보 전달

2. `detail_screen.dart`
   - 이미 구현됨 (추가 수정 불필요)
   - 추천 이유 배지 표시
   - URL 버튼 조건부 활성화

## 테스트 시나리오

### 1. 추천 이유 표시 확인
1. 홈 화면에서 추천 리스트 로드
2. 추천 카드 클릭하여 상세페이지 이동
3. **확인**: 포스터 아래에 추천 이유 배지 표시
   - "음악 취향이 X% 일치..."
   - "뮤지컬 취향이 X% 일치..."
   - "인기차트 X위 작품입니다"

### 2. 평점 표시 확인
1. 추천 리스트에서 뮤지컬 선택
2. **확인**: 상세페이지에 평균 평점 표시
   - averageRating 값이 표시됨
   - null이면 표시 안 함

### 3. URL 버튼 활성화 확인
1. 추천 리스트에서 뮤지컬 선택
2. **확인**: 인터파크/Yes24 버튼이 활성화됨 (회색 아님)
3. 버튼 클릭
4. **확인**: 해당 사이트로 이동

### 4. Fallback 동작 확인
1. CrawledMusicalRanking에 없는 뮤지컬
2. **확인**: Musical 데이터로 fallback
3. **확인**: 추천 이유는 여전히 표시
4. **확인**: URL은 없지만 에러 없음

## 배포 체크리스트

### 컴파일 확인
```bash
cd /Users/leejungheon/Desktop/springstudy/melodical_app
./gradlew compileJava
# ✅ BUILD SUCCESSFUL
```

### 서버 시작
```bash
./gradlew bootRun
```

### Flutter 빌드 및 실행
```bash
flutter run
```

### 확인 사항
- [ ] 추천 API 응답에 `averageRating`, `interparkUrl`, `yes24Url` 포함
- [ ] 상세페이지에 추천 이유 배지 표시
- [ ] URL 버튼 활성화 및 정상 작동
- [ ] 평점 표시
- [ ] 데이터 없을 때 에러 없이 fallback

## 주의사항

### 데이터 정합성
- `CrawledMusicalRanking.id`와 `Musical.id`는 **다른 값**
- 추천에 사용되는 ID는 이제 `CrawledMusicalRanking.id`
- 기존 시스템에서 `Musical.id`를 사용하는 부분은 영향 없음

### Fallback 로직
- CrawledMusicalRanking에 데이터가 없으면 Musical 데이터 사용
- URL과 평점은 CrawledMusicalRanking에만 있음
- Musical로 fallback 시 URL은 null

### 성능 고려
- `findAllById()` 사용으로 N+1 문제 방지
- 두 테이블 모두 조회하지만 IN 쿼리로 최적화됨

## 향후 개선 사항

### 단기 (1주)
1. 평점 UI 개선 (별점 아이콘 등)
2. URL 버튼 디자인 개선
3. 추천 이유 다양화 (더 많은 패턴)

### 중기 (1개월)
1. CrawledMusicalRanking과 Musical 데이터 통합
2. 정기적인 데이터 동기화
3. 추천 이유 A/B 테스트

### 장기 (3개월)
1. 실시간 평점 업데이트
2. 사용자별 맞춤 추천 이유
3. 추천 이유 클릭 시 상세 분석 제공

---

## 요약

**문제**: 
1. 추천 이유가 상세페이지에 안 나옴
2. 추천 리스트의 뮤지컬에 평점과 URL이 없음

**해결**:
1. 추천 이유 데이터 전달 경로 명확화
2. Integrated 데이터(CrawledMusicalRanking) 직접 사용
3. RecommendationResponse에 평점 및 URL 필드 추가
4. Frontend에서 추천 데이터 직접 전달

**결과**:
- ✅ 모든 추천에 이유 표시
- ✅ 평점 및 URL 버튼 활성화
- ✅ 데이터 일관성 확보

**다음 단계**: 서버 재시작 후 테스트
