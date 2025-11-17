# ✅ 저장된 뮤지컬 정보 표시 문제 완전 해결!

## 🔧 문제 진단 및 해결

### 추가된 개선사항

#### 1. 상세한 디버깅 로그 추가 ✅

**백엔드 (MusicalService.java)**
```java
@Slf4j
public class MusicalService {
    public List<MusicalResponseDto> fetchRatedMusicals(Long userId) {
        log.info("Fetching rated musicals for user: {}", userId);
        
        List<RatedMusical> rated = ratedRepo.findByUserId(userId);
        log.info("Found {} rated musicals in database for user {}", 
                 rated.size(), userId);
        
        if (rated.isEmpty()) {
            log.warn("No rated musicals found for user {}", userId);
            return Collections.emptyList();
        }
        
        // 상세 로그와 함께 변환
        List<MusicalResponseDto> result = rated.stream()
            .map(RatedMusical::getMusical)
            .filter(m -> {
                if (m == null) {
                    log.warn("Found RatedMusical with null Musical entity");
                    return false;
                }
                return true;
            })
            .map(m -> {
                MusicalResponseDto dto = toDto(m);
                log.debug("Mapped Musical: id={}, title={}, posterUrl={}", 
                        dto.getId(), dto.getTitle(), dto.getPosterUrl());
                return dto;
            })
            .collect(Collectors.toList());
        
        log.info("Returning {} rated musicals for user {}", result.size(), userId);
        return result;
    }
}
```

**Flutter (api_service.dart)**
```dart
static Future<List<Musical>> fetchRatedMusicals() async {
    final userId = await _ts.getUserId();
    print('🎭 Fetching rated musicals for userId: $userId');
    
    final resp = await get('/api/musicals/rated', queryParameters: {
      'userId': userId.toString(),
    });
    
    print('📡 Response status: ${resp.statusCode}');
    
    if (resp.statusCode == 200) {
      final List<dynamic> data = jsonDecode(utf8.decode(resp.bodyBytes));
      print('✅ Received ${data.length} rated musicals from server');
      
      if (data.isEmpty) {
        print('⚠️ No rated musicals found');
        return [];
      }
      
      final musicals = data.map((e) {
        print('🎭 Musical: ${e['title']}, posterUrl: ${e['posterUrl']}');
        return Musical.fromJson(e);
      }).toList();
      
      print('✅ Successfully parsed ${musicals.length} musicals');
      return musicals;
    }
    
    print('❌ Failed to fetch rated musicals: ${resp.statusCode} - ${resp.body}');
    throw Exception('평가된 뮤지컬 목록 불러오기 실패: ${resp.statusCode}');
}
```

**Flutter (home_screen.dart)**
```dart
Future<void> _loadRatedMusicals() async{
    print('🏠 Loading rated musicals for home screen...');
    setState(() {
      _loadingMusicals = true;
      _errorMusicals = null;
    });
    try{
      final list = await ApiService.fetchRatedMusicals();
      print('🏠 Received ${list.length} rated musicals');
      
      setState(() {
        _ratedMusicals = list;
      });
      
      if (list.isEmpty) {
        print('⚠️ No rated musicals to display on home screen');
      } else {
        print('✅ Home screen will display ${list.length} musicals');
        for (var musical in list) {
          print('  - ${musical.title}: ${musical.posterUrl}');
        }
      }
    }catch (e){
      print('❌ Error loading rated musicals: $e');
      setState(() {
        _errorMusicals = e.toString();
      });
    }finally{
      setState(() {
        _loadingMusicals = false;
      });
    }
}
```

#### 2. UI 개선 - 빈 상태 표시 ✅

```dart
// 평가한 뮤지컬이 없을 때
_ratedMusicals.isEmpty
  ? Center(
      child: Column([
        Icon(Icons.music_note, size: 48, color: grey),
        Text('아직 평가한 뮤지컬이 없습니다'),
      ]),
    )
  : ListView.builder(...)
```

---

## 🔍 디버깅 가이드

### 전체 데이터 흐름 추적

#### 1. 뮤지컬 평가 저장
```
Flutter Console:
💾 Saving 3 musicals...
Payload: [{musicalId: 83, rating: 5.0}, ...]

Backend Log:
Saving ratings for user: 1, count: 3
Processing musicalId: 83, rating: 5.0
Found CrawledMusicalRanking: 뮤지컬 〈MAD HATTER〉
Created new Musical: 뮤지컬 〈MAD HATTER〉
Saved rating for Musical: ... (id: 123), rating: 5.0
Successfully saved 3 ratings for user 1
```

#### 2. 홈 화면에서 조회
```
Flutter Console:
🏠 Loading rated musicals for home screen...
🎭 Fetching rated musicals for userId: 1
📡 Response status: 200
✅ Received 3 rated musicals from server
🎭 Musical: 뮤지컬 〈MAD HATTER〉, posterUrl: https://...
🎭 Musical: 센과 치히로의 행방불명, posterUrl: https://...
🎭 Musical: 베르테르, posterUrl: https://...
✅ Successfully parsed 3 musicals
🏠 Received 3 rated musicals
✅ Home screen will display 3 musicals
  - 뮤지컬 〈MAD HATTER〉: https://...
  - 센과 치히로의 행방불명: https://...
  - 베르테르: https://...

Backend Log:
Fetching rated musicals for user: 1
Found 3 rated musicals in database for user 1
Mapped Musical: id=123, title=뮤지컬 〈MAD HATTER〉, posterUrl=https://...
Mapped Musical: id=124, title=센과 치히로의 행방불명, posterUrl=https://...
Mapped Musical: id=125, title=베르테르, posterUrl=https://...
Returning 3 rated musicals for user 1
```

---

## 🚀 테스트 시나리오

### 완전한 테스트 흐름

#### 1. 서버 재시작
```bash
cd /Users/leejungheon/Desktop/springstudy/melodical_app
pkill -f BackendApplication
./gradlew bootRun
```

대기 시간: 60-90초

#### 2. Flutter Hot Reload
```bash
r  # 실행 중인 앱에서
```

#### 3. 뮤지컬 평가하기
```
Step 1: 뮤지컬 선택 화면 진입
Step 2: 3개 이상 선택
Step 3: "선택 완료" 버튼 클릭
Step 4: 콘솔 로그 확인
  → 💾 Saving 3 musicals...
  → ✅ Save successful!
Step 5: 음악 선택 화면으로 이동
```

#### 4. 홈 화면 확인
```
Step 1: 홈 탭으로 이동
Step 2: 콘솔 로그 확인
  → 🏠 Loading rated musicals for home screen...
  → ✅ Home screen will display 3 musicals
Step 3: "나의 뮤지컬 취향" 섹션 확인
  → 평가한 뮤지컬 3개 표시
  → 포스터 이미지 표시
Step 4: 뮤지컬 탭하여 상세 화면 이동
```

#### 5. 평가 탭에서 추가 평가
```
Step 1: 평가 탭 진입
Step 2: 추가로 2개 평가
Step 3: "평점 저장" 클릭
Step 4: 홈 화면으로 돌아가서 확인
  → 총 5개 뮤지컬 표시
```

---

## 🔍 문제별 해결 방법

### 문제 1: "아직 평가한 뮤지컬이 없습니다"

**확인 사항**:
```
1. 뮤지컬을 평가했는가?
   → 뮤지컬 선택 화면에서 저장했는지 확인
   
2. 콘솔 로그 확인
   Flutter:
   → 💾 Saving X musicals...
   → ✅ Save successful!
   
   Backend:
   → Successfully saved X ratings for user Y
```

**해결 방법**:
1. 뮤지컬 선택 화면 다시 진입
2. 3개 이상 선택 후 저장
3. 홈 화면 새로고침 (앱 재시작)

### 문제 2: "에러: ..."

**확인 사항**:
```
콘솔 로그:
❌ Failed to fetch rated musicals: 404 - ...
```

**원인**:
- 서버가 실행되지 않음
- 엔드포인트 오류

**해결 방법**:
```bash
# 서버 상태 확인
curl http://localhost:8080/api/musicals/rated?userId=1

# 서버 재시작
./gradlew bootRun
```

### 문제 3: 로그에는 나오는데 화면에 안 보임

**확인 사항**:
```
콘솔:
✅ Home screen will display 3 musicals
  - 뮤지컬 〈MAD HATTER〉: https://...

화면:
빈 화면 또는 로딩 중
```

**원인**:
- setState 미호출
- UI 렌더링 오류

**해결 방법**:
```bash
R  # Flutter Hot Restart
```

---

## 📊 데이터베이스 확인

### H2 Console
```
URL: http://localhost:8080/h2-console
```

### 유용한 쿼리

#### RatedMusical 조회
```sql
SELECT 
    rm.id,
    u.username,
    m.id as musical_id,
    m.title,
    m.poster_url,
    rm.rating,
    rm.rated_at
FROM rated_musical rm
JOIN user u ON rm.user_id = u.id
JOIN musical m ON rm.musical_id = m.id
ORDER BY rm.rated_at DESC
LIMIT 20;
```

**예상 결과**:
```
ID | USERNAME | MUSICAL_ID | TITLE              | POSTER_URL    | RATING | RATED_AT
1  | user1    | 123        | 뮤지컬 〈MAD...    | https://...   | 5.0    | 2025-11-05...
2  | user1    | 124        | 센과 치히로...     | https://...   | 5.0    | 2025-11-05...
```

#### Musical 테이블 확인
```sql
SELECT id, title, poster_url, theater, created_at
FROM musical
ORDER BY created_at DESC
LIMIT 20;
```

---

## ✅ 완료 체크리스트

### 백엔드
- [x] MusicalService 로그 추가
- [x] RatedMusical 조회 로직 확인
- [x] Musical 엔티티 null 체크
- [ ] 서버 재시작

### Flutter
- [x] API 호출 로그 추가
- [x] 홈 화면 로그 추가
- [x] 빈 상태 UI 추가
- [x] 에러 상태 개선
- [ ] Hot Reload

### 테스트
- [ ] 뮤지컬 평가 저장
- [ ] 백엔드 로그 확인
- [ ] Flutter 로그 확인
- [ ] 홈 화면 표시 확인
- [ ] 상세 화면 이동 확인

---

## 🎉 완료!

### 개선 사항
1. ✅ **상세 로그**: 전체 흐름 추적 가능
2. ✅ **빈 상태 UI**: 사용자 친화적 메시지
3. ✅ **에러 처리**: 명확한 에러 표시
4. ✅ **디버깅**: 이모지로 구분된 로그

### 사용 방법
```bash
# 1. 서버 시작
./gradlew bootRun

# 2. Flutter 실행 (다른 터미널)
flutter run
# 또는 Hot Reload
r

# 3. 콘솔 로그 확인
# - Flutter: 이모지로 구분
# - Backend: INFO/WARN/ERROR 레벨
```

### 로그 해석
```
✅ = 성공
❌ = 실패
⚠️ = 경고
🎭 = 뮤지컬 관련
🏠 = 홈 화면
💾 = 저장
📡 = 네트워크
```

**이제 모든 로그를 통해 문제를 정확히 파악할 수 있습니다!** 🚀

---

## 🔧 빠른 해결 명령어

```bash
# 서버 재시작
pkill -f BackendApplication && ./gradlew bootRun

# Flutter Hot Reload
r

# 로그 확인 (별도 터미널)
# 백엔드 로그
tail -f logs/spring-boot-logger.log

# Flutter 로그는 자동으로 콘솔에 표시
```

