# 개인 블로그 시스템 계획·설계

> 이 문서는 저장소의 `CLAUDE.md`, README, ADR, OpenAPI를 종합한 초기 설계 기준이다. 확정되지 않은 도메인·운영 결정은 마지막 절의 미결 항목으로 남긴다.

## 제품 목적

한국어 기술 글을 작성·발행하고, 방문자가 최신 글·태그·검색으로 내용을 찾고 읽을 수 있는 1인 운영 블로그를 제공한다. 백엔드는 공개 글, 댓글, 사용자 인증, 미디어 업로드를 각각의 바운디드 컨텍스트로 관리하며, 프론트엔드는 별도 프로젝트로 배포한다.

## 사용자와 핵심 흐름

- **방문자:** 발행 글을 목록·검색·태그로 찾고 상세 내용을 읽는다.
- **로그인한 독자:** Google 로그인 후 댓글을 작성하고 자신이 작성한 댓글을 삭제한다.
- **운영자(ADMIN):** 초안을 만들고 저장·발행·발행 취소·삭제·복구하며 태그와 이미지를 관리한다.

### 공개 글 흐름

1. 방문자가 `GET /articles` 또는 `GET /articles/search`로 발행 글을 찾는다.
2. `GET /articles/{articleId}`는 공개된 글만 반환하고, DRAFT·DELETED를 404로 숨긴다.
3. 목록은 `publishedAt` 내림차순 커서 페이지네이션을 사용하며, 검색도 같은 정렬 규칙을 따른다.

### 글 작성·발행 흐름

1. ADMIN이 빈 DRAFT를 만든다.
2. 초안 저장은 `version`을 사용한 낙관적 동시성 제어를 한다. 요청 버전이 다르면 `ARTICLE_VERSION_CONFLICT`로 저장을 거부한다.
3. 발행은 제목과 본문이 유효한지 확인한 뒤 공개 상태로 전환한다. 이미 발행된 글은 재발행할 수 있다.
4. 발행 취소는 공개 접근을 막되 기존 초안을 보존한다. 삭제는 소프트 삭제이며, 필요하면 복구한다.

## 아키텍처

### 시스템 형태

- Java 21과 Spring Boot 기반 모듈러 모놀리스다.
- 바운디드 컨텍스트마다 `domain`, `usecase`, `query`, `outbound`를 두는 헥사고날 구조를 따른다.
- 의존 방향은 `adapter → core → libs` 하나다. core는 Spring Web, HTTP, 외부 SDK, 다른 컨텍스트 코드를 가져오지 않는다.
- 프론트엔드는 Next.js로 별도 배포한다. 백엔드를 먼저 배포하므로 API 변경은 하위 호환을 유지한다.

### 모듈 경계

| 영역 | 책임 |
| --- | --- |
| `libs/core-kernel` | 애그리거트 기반, 도메인 이벤트, 비즈니스 예외와 에러 코드 |
| `libs/id-generator` | 53비트 Snowflake ID 생성 포트와 구현 |
| `libs/rdb-kernel` | RDB 공통 지원. 실제 필요성이 생긴 뒤 추가 |
| `libs/web-kernel` | RFC 9457 응답, 인증 주체, 전역 예외 처리 |
| `libs/events` | 컨텍스트 간 공개 이벤트 계약 |
| `service/article` | 글 초안·발행·목록·검색·태그 관리 |
| `service/comment` | 댓글·대댓글·삭제 정책 |
| `service/user-auth` | Google OAuth, 사용자·권한, access/refresh token |
| `service/media` | 업로드 승인·확인과 미디어 메타데이터 |
| `mains/monolith-main` | 모듈 조립, Spring 설정, 앱 진입점만 담당 |

타깃 모듈 구조는 `service/{context}/{context}-core`와 `service/{context}/adapter/{context}-in-{http|acl}`, `{context}-out-{technology}`다. 아직 빌드에 포함되지 않은 모듈은 설계 목표이며 현재 구현 상태로 간주하지 않는다.

### 컨텍스트 간 연결

- 다른 컨텍스트의 core 패키지를 직접 참조하지 않는다.
- 연동이 필요하면 호출하는 쪽의 outbound 어댑터가 대상의 `in-acl` 모듈만 호출한다. ACL은 자체 DTO를 공개하고 대상 core를 `implementation` 의존성으로 둔다.
- 애그리거트 사이에는 ID만 보관하고, 다른 스키마 테이블을 직접 조인하거나 스키마 간 FK를 만들지 않는다.
- 컨텍스트 간 이벤트 타입은 `libs/events`에만 둔다. 메시지 브로커 추가는 현재 설계 범위가 아니다.

## 도메인 모델

### Article

- 상태는 `DRAFT`, `PUBLISHED`, `DELETED`다.
- 글은 draft 본문과 공개본을 분리한다. 초안 저장 시 버전 충돌을 거부하고, 발행 시 공개본을 갱신한다.
- 상태 변경은 애그리거트 메서드만 허용한다. 조회용 `canPublish()`와 실행용 `publish()`처럼 규칙 확인과 명령을 함께 표현한다.
- 공개 조회는 PUBLISHED 글만 대상으로 한다. API 요약은 제목, 요약, 선택적 썸네일, 태그, 카테고리 경로(없으면 `null`), 발행 시각을 제공한다.

### Tag

- 태그는 이름과 ID를 가지며 글에서 참조한다.
- 글 태그 교체 API는 요청 이름 목록으로 전체를 대체하고, 없는 이름은 새 태그로 만든다.
- 글당 최대 10개, 태그 이름은 1~30자다.

### Category

- 카테고리는 article 컨텍스트에 둔다. 글 목록을 카테고리로 거르려면 글과 같은 스키마(`blog_article`)에 있어야 하고, 컨텍스트 간 FK는 만들지 않기 때문이다.
- 상위·하위 2단계 트리이며 같은 부모 안의 순서(`position`)와 공개 여부(`visible`)를 가진다. 슬러그는 전체에서 유일하다.
- 글은 카테고리 하나에 속하거나 미분류다. 태그는 카테고리와 별개로 계속 쓴다.
- 하위 카테고리가 있으면 삭제할 수 없고, 글이 있으면 다른 카테고리로 옮긴 뒤 삭제한다.
- 계약은 OpenAPI에 정의했고 도메인·저장소·엔드포인트 구현은 아직 없다(설계 초안).

### Comment

- 댓글은 글 ID와 작성자 ID를 값으로 참조한다.
- 최상위 댓글과 한 단계 대댓글을 지원한다. 대댓글에는 다시 답글을 달 수 없다.
- 삭제된 댓글은 대댓글이 있으면 자리를 유지한다. 다른 사용자의 댓글은 작성자나 ADMIN만 삭제할 수 있다.
- 댓글 작성은 로그인 필요, 요청 과다 시 429를 반환한다.

### UserAuth

- Google OAuth2로 로그인하고, ADMIN 역할을 가진 사용자는 관리 API를 이용한다.
- access token은 Bearer JWT, refresh token은 HttpOnly 쿠키로 전달한다.
- refresh token은 사용할 때마다 회전한다. 이미 회전된 토큰이 다시 오면 해당 사용자의 refresh token을 모두 폐기한다.

### Media

- 업로드 URL 발급으로 PENDING 미디어 레코드와 10분 유효한 presigned URL을 생성한다.
- 업로드 완료 확인은 저장 객체 존재 여부와 신고한 파일 크기를 확인한 뒤 CONFIRMED로 전환한다.
- 확정된 미디어만 글 본문 또는 썸네일에서 참조한다.

## 저장소와 데이터

- MySQL 8.4 LTS를 로컬 기준으로 사용한다. 스키마는 `blog_article`, `blog_comment`, `blog_auth`, `blog_media`로 분리한다.
- 컨텍스트 스키마 사이에 FK를 만들지 않는다. 다른 컨텍스트의 ID는 값으로 보관한다.
- Flyway는 컨텍스트별 리소스 경로에 버전 마이그레이션을 둔다. 이미 머지된 마이그레이션은 수정하지 않고 새 버전을 추가한다.
- JPA `ddl-auto`는 `validate`다. 스키마 생성의 정본은 마이그레이션이다.
- MySQL FULLTEXT ngram parser로 한글 검색을 제공한다. MVP 검색은 관련도 정렬이 아니라 발행일 내림차순이다.
- 컬럼 삭제·이름 변경은 확장 → 전환 → 축소 순서로 진행해 직전 애플리케이션 버전과 호환되게 한다.

## API·오류·보안

- API 기준은 `docs/api/openapi.yaml`이다. 공통 경로는 `/api/v1`, 관리자 경로는 `/api/v1/admin`이다.
- 실패는 RFC 9457 Problem Details `application/problem+json`으로 표현하고 확장 필드 `code`, `requestId`를 제공한다.
- 입력 검증 실패는 400, 도메인 충돌은 409, 미인증은 401, 권한 없음은 403, 공개되지 않은 글은 404로 응답한다.
- 시간은 UTC `Instant`, 응답은 ISO 8601 문자열을 사용한다. ID는 53비트 JSON number다.
- 시크릿은 환경 설정으로 전달한다. API 키, OAuth 비밀, 실제 토큰을 코드·테스트·로그에 남기지 않는다.
- 관리 권한은 화면 라우트가 아니라 API가 최종 확인한다.
- API 호환을 깨는 변경은 명시적인 승인과 버전 정책 없이 배포하지 않는다.

## 품질·운영 기준

- 도메인 규칙은 Spring 없이 순수 단위 테스트로 검증한다. 유스케이스는 fake/Mockito 포트를 사용하고, 어댑터·API 테스트는 공유 Testcontainers를 이용한다.
- `./gradlew build`에 포맷, 정적 분석, 커버리지 게이트가 포함된다. core 라인·브랜치 커버리지 목표는 80%다.
- 모듈 간 의존 규칙은 ArchUnit으로 검사한다. 규칙을 완화해 테스트를 통과시키지 않는다.
- readiness health check를 제공하고 `X-Request-Id`를 사용해 요청 오류를 추적한다.
- 메트릭은 Prometheus, 요청별 상세 접근 이력은 구조화 로그로 분리하는 관측성 목표를 둔다. 접근 로그 필드, 민감 정보 제한, 수집 경로는 [`../operations/observability.md`](../operations/observability.md)에 정의한다. 이는 현재 운영 구성이나 설치 완료를 의미하지 않는다.
- 로컬 개발·통합 테스트에는 Docker MySQL/Testcontainers를 사용한다.

## 구현 순서 제안

1. 커널·ID 생성기·빌드 규칙을 안정화한다.
2. Article 핵심 모델과 저장·조회 경계를 만들고 공개 읽기 API를 구현한다.
3. ADMIN 인증과 초안·발행 API를 구현한다.
4. Tag 조회/편집과 한글 검색을 연결한다. Category 트리·관리 API와 글 카테고리 지정을 같은 article 컨텍스트에 추가한다.
5. Comment 정책과 인증 흐름을 추가한다.
6. Media presigned 업로드와 프론트 이미지 허용 호스트를 확정한다.

각 단계는 하나의 범위로 나누고 선행 API가 배포된 뒤 다음 컨텍스트를 연결한다. 이 순서는 현재 모듈 구현 완료를 뜻하지 않는다.

## 미결 항목

- ADMIN을 최초로 부여하는 방법과 허용 Google 계정 목록
- 실제 API·프론트·이미지 도메인 및 CORS 정책
- refresh 쿠키의 SameSite·Domain 설정과 운영 환경 간 배포 구성
- presigned storage 사업자, 버킷 경로, 공개 URL·삭제·고아 업로드 정리 정책
- OpenAPI는 댓글 목록 설명에서 HIDDEN 댓글을 제외한다고 하지만 `CommentStatus` 열거형에는 ACTIVE·DELETED만 있다. 숨김 상태 모델을 확정해야 한다.
- 댓글 신고·운영자 숨김/복구 정책
- 현재 대표 이메일·표시 이름, SEO 도메인, 서비스 소개 문구
- API 스펙의 `api.example.com` 및 예시 미디어 URL을 실제 도메인으로 교체하는 시점
