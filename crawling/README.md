# Musical Ranking Crawler

뮤지컬 인기 순위 데이터 수집 시스템 - 인터파크와 Yes24 티켓 사이트에서 뮤지컬 정보를 크롤링합니다.

## 주요 기능

- **주간/월간 뮤지컬 랭킹 수집**
- **상세 정보 100% 수집** (인터파크 기준):
  - 순위 및 평점
  - 이미지 URL (100%)
  - 상세 페이지 URL
  - 공연 기간 (98%)
  - 예매율 (100%)
  - 공연장 (100%)
  - 캐스팅 정보
- **데이터 통합**: 두 사이트 데이터를 자동으로 매칭하여 통합
- **CSV/JSON 형식 저장**
- **윤리적 크롤링**: Rate limiting 및 에러 처리

## 프로젝트 구조

```
crawling/
├── src/
│   ├── crawlers/
│   │   ├── __init__.py
│   │   ├── interpark_improved.py    # 인터파크 크롤러 (개선 버전)
│   │   └── yes24_new.py             # Yes24 크롤러
│   ├── models/                       # 데이터 모델
│   ├── utils/                        # 유틸리티 함수
│   ├── integrator.py                 # 데이터 통합 모듈
│   └── main.py                       # 메인 실행 파일
├── data/                             # 수집된 데이터 (CSV/JSON)
├── logs/                             # 로그 파일
├── requirements.txt                  # Python 패키지 목록
└── README.md
```

## 설치 방법

1. 저장소 클론:
```bash
git clone <repository-url>
cd crawling
```

2. 가상환경 생성:
```bash
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
```

3. 패키지 설치:
```bash
pip install -r requirements.txt
```

## 사용 방법

메인 크롤러 실행:
```bash
python src/main.py
```

크롤러가 인터파크와 Yes24에서 데이터를 수집하고, 통합하여 `data/` 디렉토리에 저장합니다.

### 출력 파일

- `interpark_weekly_YYYYMMDD_HHMMSS.csv/json` - 인터파크 주간 랭킹
- `interpark_monthly_YYYYMMDD_HHMMSS.csv/json` - 인터파크 월간 랭킹
- `yes24_weekly_YYYYMMDD_HHMMSS.csv/json` - Yes24 주간 랭킹
- `yes24_monthly_YYYYMMDD_HHMMSS.csv/json` - Yes24 월간 랭킹
- `integrated_weekly_dataset_YYYYMMDD_HHMMSS.csv/json` - **통합 주간 데이터**
- `integrated_monthly_dataset_YYYYMMDD_HHMMSS.csv/json` - **통합 월간 데이터**

## 수집 대상 사이트

- **인터파크**: https://tickets.interpark.com/contents/ranking?genre=MUSICAL
- **Yes24**: https://ticket.yes24.com/Genre/Musical

## 데이터 필드

통합 데이터셋에는 다음 정보가 포함됩니다:

| 필드 | 설명 | 출처 |
|------|------|------|
| title | 뮤지컬 제목 | 인터파크 우선 |
| rank | 최종 순위 | 통합 순위 |
| interpark_rank | 인터파크 순위 | 인터파크 |
| yes24_rank | Yes24 순위 | Yes24 |
| interpark_rating | 인터파크 평점 | 인터파크 |
| yes24_rating | Yes24 평점 (5점 만점) | Yes24 |
| image_url | 포스터 이미지 URL | 인터파크 (100%) |
| detail_url | 인터파크 상세 페이지 | 인터파크 |
| yes24_detail_url | Yes24 상세 페이지 | Yes24 |
| performance_period | 공연 기간 | 인터파크 (98%) |
| booking_rate | 예매율 (%) | 인터파크 (100%) |
| venue | 공연장 | 인터파크 (100%) |
| source | 데이터 출처 | both/interpark/yes24 |

## 수집 성능

### 인터파크 (개선 버전)
- ✅ 이미지 URL: **100%** (50/50개)
- ✅ 예매율: **100%** (50/50개)
- ✅ 공연 기간: **98%** (49/50개)
- ✅ 공연장: **100%** (50/50개)

### 통합 결과
- 총 수집: 약 **80-90개** 뮤지컬
- 양쪽 매칭: 약 **10개**
- 필수 정보 완비: **50개 이상** (60%+)

## 기술 스택

- **Python 3.8+**
- **Selenium**: 동적 페이지 렌더링
- **BeautifulSoup4**: HTML 파싱
- **Requests**: HTTP 요청
- **Pandas**: 데이터 처리 및 저장

## 라이선스

이 프로젝트는 교육 목적으로 제작되었습니다. 대상 웹사이트의 이용 약관을 준수해 주세요.
