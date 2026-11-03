# 개인 블로그 백엔드

모듈러 모놀리스로 만드는 개인 블로그 백엔드다. 바운디드 컨텍스트마다 헥사고날 구조를 따른다. 지금은 P0(골격) 단계로, 공통 커널과 ID 생성기, 빈 Spring Boot 앱이 들어 있다.

설계 문서, 아키텍처 규칙, 커밋 규칙은 `CLAUDE.md`와 `docs/`에 있다. 운영 지표와 상세 접근 로그 설계는 [`docs/operations/observability.md`](docs/operations/observability.md)를 참고한다.

## 준비물

- JDK 21
- Docker (로컬 MySQL, 통합 테스트용)

## 처음 한 번

```bash
git init -b main
git config core.hooksPath githooks        # pre-commit(포맷), pre-push(단위 테스트)
cp .env.example .env                       # 비밀번호를 바꾼다
docker compose up -d mysql
./gradlew build
```

## 자주 쓰는 명령

| 할 일                                        | 명령                                                                             |
| -------------------------------------------- | -------------------------------------------------------------------------------- |
| 전체 빌드 (테스트, 정적 분석, 커버리지 검증) | `./gradlew build`                                                                |
| 단위 테스트                                  | `./gradlew unitTest`                                                             |
| 통합 테스트 (Docker 필요)                    | `./gradlew integrationTest`                                                      |
| 포맷 적용                                    | `./gradlew spotlessApply`                                                        |
| 앱 실행                                      | `./gradlew :mains:monolith-main:bootRun --args='--spring.profiles.active=local'` |
| 헬스 체크                                    | `curl localhost:8080/actuator/health/readiness`                                  |

## 구조

```
backend/
├─ buildSrc/                 # 컨벤션 플러그인 (java, spring, coverage)
├─ gradle/libs.versions.toml # 버전 카탈로그
├─ config/                   # Checkstyle, SpotBugs 설정
├─ libs/
│  ├─ core-kernel/           # AggregateRoot, DomainEvent, ErrorCode, BusinessException
│  └─ id-generator/          # 53비트 Snowflake ID 생성기
├─ mains/monolith-main/      # 조립과 설정
├─ docker/mysql/init/        # 스키마 4개 생성 스크립트
├─ githooks/                 # pre-commit, pre-push
└─ docs/                     # API 계약, ADR, 설계 문서, 운영 관측성, ERD·스키마 정의
```

## 버전

착수 시점(2026-11-03)의 최신 안정판 중, 릴리스 후 1주 이상 지난 버전으로 골랐다.

| 항목                     | 버전    | 릴리스     |
| ------------------------ | ------- | ---------- |
| Gradle                   | 9.8.2   | 2026-10-19 |
| Spring Boot              | 4.1.2   | 2026-10-22 |
| Spotless Gradle 플러그인 | 8.10.4  | 2026-10-08 |
| google-java-format       | 1.37.0  | 2026-10-01 |
| SpotBugs Gradle 플러그인 | 6.5.13  | 2026-10-12 |
| Checkstyle               | 14.3.0  | 2026-09-27 |
| JaCoCo                   | 0.8.16  | 2026-10-26 |
| MySQL (로컬)             | 8.4 LTS | —          |
