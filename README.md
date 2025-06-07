## 🏗️ 기술 스택

| 분야       | 스택                                                      |
|------------|-----------------------------------------------------------|
| Backend    | Java 21, Spring Boot 3.5.0, Spring Security, JWT, JPA    |
| Database   | AWS RDS (MySQL)                                           |
| Infra      | AWS EC2, GitHub Actions (CI/CD 예정)                      |
| Tooling    | IntelliJ, Git, GitHub, Figma                              |

---

## 🔐 백엔드 주요 기능

- ✅ 회원가입 / 로그인 (JWT 인증)
- ✅ DB 연동 (AWS RDS + JPA)
- ✅ 비밀번호 암호화 (BCrypt)
- ✅ 사용자 데이터 저장 및 조회 API

---

## 📁 폴더 구조

```bash
backend/
├── src/main/java/com/melodical/backend/
│   ├── config          # 보안 설정 및 JWT
│   ├── controller      # API 엔드포인트
│   ├── dto             # 요청/응답 객체
│   ├── entity          # JPA 엔티티
│   └── service         # 비즈니스 로직
└── src/main/resources/
    └── application.yml # DB 설정
