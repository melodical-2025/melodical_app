# 🎭 Melodical - 뮤지컬 추천 플랫폼

음악 취향 기반의 지능형 뮤지컬 추천 시스템입니다. 사용자의 음악 선호도를 분석하여 맞춤형 뮤지컬 공연을 추천합니다.

## 📋 프로젝트 개요

Melodical은 음악 취향과 뮤지컬 관람 이력을 기반으로 사용자에게 최적화된 뮤지컬 공연을 추천하는 모바일 애플리케이션입니다. 3단계 추천 알고리즘(Stage-1: Candidate Generation, Stage-2: pCTR Ranking, Stage-3: Twiddler)을 통해 정확하고 다양한 추천을 제공합니다.

## 🏗️ 기술 스택

### Backend
- **Framework**: Spring Boot 3.5.0
- **Language**: Java 21
- **Database**: MySQL
- **Cache**: Redis (Lettuce)
- **Security**: Spring Security + OAuth2 + JWT
- **Social Login**: Google, Kakao, Naver, Apple

### Frontend
- **Framework**: Flutter 3.3.2+
- **Language**: Dart
- **State Management**: Provider
- **Storage**: Flutter Secure Storage

## 🎯 주요 기능

### 1. 소셜 로그인
- Google, Kakao, Naver, Apple 로그인 지원
- JWT 기반 인증 시스템

### 2. 음악/뮤지컬 취향 분석
- 사용자의 음악 선호도 수집 및 분석
- 뮤지컬 관람 이력 기반 프로필 생성

### 3. 3단계 추천 알고리즘
- **Stage-1**: Candidate Generation (음악 취향 CF, 뮤지컬 취향 CF, 인기차트 기반)
- **Stage-2**: pCTR Ranking (클릭 확률 예측 모델)
- **Stage-3**: Twiddler (다양성 보장 및 최종 순위 조정)

### 4. 실시간 공연 정보
- 인터파크, Yes24 크롤링 데이터 통합
- 공연 일정, 가격, 극장 정보 제공

### 5. 추천 디버그 API
- 단계별 추천 결과 분석
- JSON/CSV 내보내기 기능
- 추천 이유 및 유사도 정보 제공

## 📁 프로젝트 구조

```
melodical_app/
├── src/main/java/com/melodical/backend/    # Spring Boot Backend
│   ├── config/                              # 설정 (Security, Redis, etc.)
│   ├── controller/                          # REST API Controllers
│   ├── dto/                                 # Data Transfer Objects
│   ├── entity/                              # JPA Entities
│   ├── repository/                          # JPA Repositories
│   ├── service/                             # Business Logic
│   │   ├── recommendation/                  # 추천 알고리즘
│   │   └── model/                           # pCTR 모델
│   └── security/                            # JWT, OAuth2 설정
├── lib/                                     # Flutter Frontend
│   ├── config/                              # API 설정
│   ├── models/                              # Data Models
│   ├── repositories/                        # API 통신
│   ├── screens/                             # UI Screens
│   ├── services/                            # Business Logic
│   └── widgets/                             # Reusable Widgets
└── crawling/                                # 공연 정보 크롤러 (Python)

```

## 🚀 시작하기

### Prerequisites
- Java 21
- MySQL 8.0+
- Redis 7.0+
- Flutter 3.3.2+
- Dart SDK

### Backend 실행

1. MySQL 및 Redis 실행
```bash
# MySQL 실행
mysql.server start

# Redis 실행
redis-server
```

2. 환경 변수 설정 (`application.properties`)
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/melodical_db
spring.datasource.username=your_username
spring.datasource.password=your_password

spring.data.redis.host=localhost
spring.data.redis.port=6379

# OAuth2 설정
spring.security.oauth2.client.registration.google.client-id=your_google_client_id
spring.security.oauth2.client.registration.google.client-secret=your_google_client_secret
# ... (Kakao, Naver, Apple 설정)
```

3. Backend 빌드 및 실행
```bash
./gradlew bootRun
```

### Frontend 실행

1. 의존성 설치
```bash
flutter pub get
```

2. API 엔드포인트 설정 (`lib/config/api_config.dart`)
```dart
static const String baseUrl = 'http://your-server:8080';
```

3. 앱 실행
```bash
# Android
flutter run

# iOS
flutter run -d ios
```

## 📊 추천 알고리즘

### Stage-1: Candidate Generation
- **음악 취향 기반 CF**: 유사한 음악 취향을 가진 사용자들이 선호하는 뮤지컬 추천
- **뮤지컬 취향 기반 CF**: 유사한 뮤지컬 관람 이력을 가진 사용자들의 선호작 추천
- **인기도 기반**: 현재 인기 있는 공연 추천

### Stage-2: pCTR Ranking
- 사용자 피처, 아이템 피처, 교차 피처, 컨텍스트 피처를 활용한 클릭 확률 예측
- LightGBM 모델 기반 정밀 랭킹

### Stage-3: Twiddler
- 다양성 보장 (음악 취향, 뮤지컬 취향, 인기도 균형)
- 신선도 부스트 (최근 등록된 공연 가산점)
- 중복 제거 및 안정적 정렬

## 🔍 API 문서

### 추천 API
- `GET /api/recommendations` - 개인화 추천 목록
- `GET /api/recommendations/debug/all-stages` - 단계별 추천 결과
- `GET /api/recommendations/debug/export` - 추천 결과 내보내기

### 인증 API
- `POST /api/auth/google` - Google 로그인
- `POST /api/auth/kakao` - Kakao 로그인
- `POST /api/auth/naver` - Naver 로그인
- `POST /api/auth/apple` - Apple 로그인

### 공연 정보 API
- `GET /api/musicals` - 뮤지컬 목록
- `GET /api/musicals/{id}` - 뮤지컬 상세 정보
- `GET /api/musicals/search` - 뮤지컬 검색

## 📸 스크린샷

<details>
<summary>앱 화면 보기</summary>

- 로그인 & 회원가입 화면
  
  <img width="1080" height="2424" alt="Screenshot_20251205_115720" src="https://github.com/user-attachments/assets/36aa747c-5221-4659-877c-c4ac01b702d8" />

- 뮤지컬 선택 화면

  <img width="1080" height="2424" alt="Screenshot_20251205_115831" src="https://github.com/user-attachments/assets/eb1e24ff-5dc0-4175-b6d0-794020384e67" />

- 음악 선택 화면
  
  <img width="1080" height="2424" alt="Screenshot_20251205_115849" src="https://github.com/user-attachments/assets/adf7970e-47c6-4572-8aaa-222d4e3afe55" />

- 홈화면
  
  <img width="1080" height="2424" alt="Screenshot_20251205_120057" src="https://github.com/user-attachments/assets/fcb4d602-b5a5-4a49-85c9-7321826aa362" />

- 뮤지컬 작품 상세 화면
  
<img width="1080" height="2424" alt="Screenshot_20251205_120116" src="https://github.com/user-attachments/assets/52d86c5e-30a0-4983-a50b-9e3c17331c52" />

- 게시판 화면
  
  <img width="1080" height="2424" alt="Screenshot_20251205_120133" src="https://github.com/user-attachments/assets/5c9a5a79-4250-4b25-8e42-d78f71fe4014" />

- 검색 화면

<img width="1080" height="2424" alt="Screenshot_20251205_123001" src="https://github.com/user-attachments/assets/3b858472-d253-45ef-9dbf-fa3dac48c628" />

- 평가 화면
  
  <img width="1080" height="2424" alt="Screenshot_20251205_120214" src="https://github.com/user-attachments/assets/c9c305b9-8f31-44a8-98d5-89d5482b51de" />

  <img width="1080" height="2424" alt="Screenshot_20251205_120220" src="https://github.com/user-attachments/assets/1298dbfc-ef1a-4cc7-88a8-d06db1f574c5" />

- 마이페이지

<img width="1080" height="2424" alt="Screenshot_20251205_123132" src="https://github.com/user-attachments/assets/1fd58f66-62fa-4442-a98f-2577de4d47b2" />

</details>

## 📝 라이센스

이 프로젝트는 학습 목적으로 제작되었습니다
