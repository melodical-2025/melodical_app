# 추천 시스템 디버깅 및 개선 완료

## 수정 사항

### 1. ✅ 홈 화면 포스터 이미지 개선

**문제**: 추천 뮤지컬 포스터가 제대로 보이지 않음

**해결**:
- 포스터 크기 조정: 100x100 → 100x130 (세로로 더 길게)
- 로딩 인디케이터 추가
- 에러 처리 개선 (빈 URL 체크 추가)
- 제목 최대 2줄 표시로 변경

**변경 파일**: `lib/screens/home_screen.dart`

```dart
// BEFORE
width: 100, height: 100

// AFTER  
width: 100, height: 130
+ loadingBuilder 추가
+ 빈 URL 체크: rec['posterUrl'] != null && rec['posterUrl'].toString().isNotEmpty
+ maxLines: 2
```

### 2. ✅ 상세 페이지 평점 표시 개선

**문제**: 추천된 뮤지컬 상세페이지에 평점이 표시되지 않음

**원인**: 
- API는 `averageRating` 제공
- DetailScreen은 `interparkRating`, `yes24Rating` 사용
- 데이터 매핑 불일치

**해결**:
```dart
// averageRating 우선, 없으면 개별 평점 fallback
final averageRating = widget.musicalData['averageRating'];
final interparkRating = widget.musicalData['interparkRating'] ?? averageRating;
final yes24Rating = widget.musicalData['yes24Rating'] ?? averageRating;
```

**변경 파일**: `lib/screens/detail_screen.dart`

### 3. ✅ URL 버튼 디버깅 추가

**문제**: 인터파크/Yes24 URL 버튼이 비활성화됨

**해결**: 상세한 디버깅 로그 추가
- URL 존재 여부
- URL 타입
- URL이 비어있는지 확인

```dart
print('🔗 Interpark URL: $interparkUrl (type: ${interparkUrl?.runtimeType})');
print('🔗 Yes24 URL: $yes24Url (type: ${yes24Url?.runtimeType})');
```

**변경 파일**: `lib/screens/detail_screen.dart`

### 4. ✅ 추천 이유 표시 개선

**문제**: 추천 이유가 상세페이지에 표시되지 않음

**해결**:
1. **home_screen.dart**: 추천 클릭 시 디버깅 로그 추가
   ```dart
   print('🎯 Recommendation clicked:');
   print('  - recommendationReason: ${rec['recommendationReason']}');
   print('  - similarityPercentage: ${rec['similarityPercentage']}');
   print('  - chartRanking: ${rec['chartRanking']}');
   ```

2. **detail_screen.dart**: 
   - initState에 데이터 수신 확인 로그 추가
   - 추천 이유 표시 조건 강화
   ```dart
   if (widget.musicalData['recommendationReason'] != null && 
       widget.musicalData['recommendationReason'].toString().isNotEmpty)
   ```

## 디버깅 로그 위치

### Home Screen (추천 클릭 시)
```
🎯 Recommendation clicked:
  - musicalId: XXX
  - title: XXX
  - posterUrl: XXX
  - recommendationReason: XXX
  - similarityPercentage: XXX
  - chartRanking: XXX
  - averageRating: XXX
  - interparkUrl: XXX
  - yes24Url: XXX
📦 Passing to DetailScreen: [list of keys]
```

### Detail Screen (초기화 시)
```
=== DetailScreen initialized ===
musicalData keys: [list of all keys]
recommendationReason: XXX
similarityPercentage: XXX
chartRanking: XXX
averageRating: XXX
interparkUrl: XXX
yes24Url: XXX
posterUrl: XXX
================================

🎭 Detail Screen - Musical: XXX (ID: XXX)
🔗 Interpark URL: XXX (type: String, isEmpty: false)
🔗 Yes24 URL: XXX (type: String, isEmpty: false)
⭐ Average Rating: X.X
⭐ Interpark Rating: X.X
⭐ Yes24 Rating: X.X
📦 Full musical data keys: [...]
```

## 테스트 시나리오

### 1. 포스터 이미지 확인
1. Flutter 앱 실행
2. 홈 화면으로 이동
3. "당신을 위한 추천" 섹션 확인
4. **예상 결과**: 
   - 포스터가 100x130 크기로 표시됨
   - 이미지 로딩 중 인디케이터 표시
   - 에러 시 아이콘 표시

### 2. 추천 이유 확인
1. 추천 뮤지컬 포스터 클릭
2. 콘솔에 "🎯 Recommendation clicked" 로그 확인
3. 상세 페이지 진입
4. 콘솔에 "=== DetailScreen initialized ===" 로그 확인
5. **예상 결과**:
   - 포스터 아래에 추천 이유 배지 표시
   - 예: "음악 취향이 85% 일치하는 사용자가 좋아하는 작품입니다"
   - 예: "인기차트 5위 작품입니다"

### 3. 평점 확인
1. 추천 뮤지컬 상세 페이지
2. 평점 섹션 확인
3. **예상 결과**:
   - 인터파크 평점 표시 (N/A가 아님)
   - Yes24 평점 표시 (N/A가 아님)
   - 콘솔에 "⭐ Average Rating" 로그 확인

### 4. URL 버튼 확인
1. 추천 뮤지컬 상세 페이지
2. 예매처 바로가기 섹션 확인
3. 콘솔에 "🔗 Interpark URL", "🔗 Yes24 URL" 로그 확인
4. **예상 결과**:
   - 버튼이 활성화됨 (회색 아님, 흰색 배경)
   - 클릭 시 해당 사이트로 이동
   - URL이 없으면 버튼 비활성화 (회색)

## 문제 해결 가이드

### 문제 1: 추천 이유가 여전히 표시되지 않음

**체크리스트**:
1. 콘솔에서 "🎯 Recommendation clicked" 로그 확인
   - `recommendationReason`이 null인지 확인
   - null이면 → 백엔드 API 문제

2. 콘솔에서 "=== DetailScreen initialized ===" 로그 확인
   - `recommendationReason`이 받아졌는지 확인
   - 받아졌는데 표시 안 됨 → UI 조건 문제

3. 백엔드 API 응답 확인:
   ```bash
   curl -X POST http://localhost:8080/api/app/recommendations \
     -H "Content-Type: application/json" \
     -d '{"userId":1,"surface":"home","count":10}'
   ```
   - `recommendationReason` 필드가 있는지 확인

### 문제 2: 평점이 N/A로 표시됨

**체크리스트**:
1. 콘솔에서 "⭐ Average Rating" 로그 확인
   - null이면 → 백엔드에서 `averageRating` 제공하지 않음

2. 백엔드 RecommendationController 확인:
   ```java
   map.put("averageRating", rec.getAverageRating());
   ```

3. RecommendationService 확인:
   ```java
   if (crawledData != null) {
       builder.averageRating(crawledData.getAverageRating())
   }
   ```

### 문제 3: URL 버튼이 비활성화됨

**체크리스트**:
1. 콘솔에서 "🔗 Interpark URL" 로그 확인
   - null 또는 empty → 백엔드 데이터 문제

2. home_screen에서 전달되는 데이터 확인:
   ```dart
   'interparkUrl': rec['interparkUrl'],
   'yes24Url': rec['yes24Url'],
   ```

3. 백엔드 Stage1CandidateService 확인:
   - CrawledMusicalRanking ID 사용하는지 확인
   - KOPIS Musical로 변환하지 않는지 확인

## 다음 단계

1. **서버 재시작 필수**:
   ```bash
   cd /Users/leejungheon/Desktop/springstudy/melodical_app
   ./gradlew bootRun
   ```

2. **Flutter 핫 리로드**:
   ```bash
   # 터미널에서 'r' 입력
   # 또는 완전 재시작: 'R' 입력
   ```

3. **테스트 진행**:
   - 홈 화면 → 추천 섹션 확인
   - 추천 포스터 클릭
   - 콘솔 로그 확인
   - 상세 페이지에서 모든 정보 확인

4. **로그 분석**:
   - 각 단계의 로그를 확인하여 데이터 흐름 추적
   - 문제 발생 시 로그를 통해 정확한 위치 파악

## 예상 결과

### 성공 시 콘솔 로그 예시
```
🎯 Recommendation clicked:
  - musicalId: 123
  - title: 위키드
  - posterUrl: http://...
  - recommendationReason: 음악 취향이 85% 일치하는 사용자가 좋아하는 작품입니다
  - similarityPercentage: 85
  - chartRanking: null
  - averageRating: 4.5
  - interparkUrl: https://ticket.interpark.com/...
  - yes24Url: https://ticket.yes24.com/...

=== DetailScreen initialized ===
musicalData keys: [id, title, posterUrl, theater, startDate, endDate, period, genre, rating, recommendationReason, similarityPercentage, chartRanking, averageRating, interparkUrl, yes24Url]
recommendationReason: 음악 취향이 85% 일치하는 사용자가 좋아하는 작품입니다
similarityPercentage: 85
chartRanking: null
averageRating: 4.5
interparkUrl: https://ticket.interpark.com/...
yes24Url: https://ticket.yes24.com/...

🎭 Detail Screen - Musical: 위키드 (ID: 123)
🔗 Interpark URL: https://ticket.interpark.com/... (type: String, isEmpty: false)
🔗 Yes24 URL: https://ticket.yes24.com/... (type: String, isEmpty: false)
⭐ Average Rating: 4.5
⭐ Interpark Rating: 4.5
⭐ Yes24 Rating: 4.5
```

### UI 표시 예상
- ✅ 포스터: 깔끔하게 표시 (100x130)
- ✅ 추천 이유 배지: 주황색 배경, 하트 또는 별 아이콘
- ✅ 평점: 4.5 / 4.5 (인터파크/Yes24)
- ✅ URL 버튼: 흰색 배경, 활성화 상태, 클릭 가능
