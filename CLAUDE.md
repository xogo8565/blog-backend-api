# 개인 블로그 플랫폼

<!-- 유지 규칙: 200줄 이하. 같은 지적을 두 번 하게 되면 한 줄 추가. 일부 경로에만 해당하는 규칙은 .claude/rules/ 로 옮긴다. -->
<!-- 패키지 com.example.blog 는 P0에서 실제 값으로 바꾼 뒤 이 파일도 함께 고친다. -->

1인 운영 개인 블로그 백엔드. 모듈러 모놀리스이고, 바운디드 컨텍스트마다 헥사고날 구조를 따른다.
이 레포는 백엔드만 담는다. 프론트엔드(Next.js)는 별도 git 레포이며, 로컬에서는 옆 폴더 `../frontend`에 있다.

- 스택: Java 21, Spring Boot, Gradle(Kotlin DSL, buildSrc 컨벤션 플러그인), MySQL 8, Flyway, Spring Data JPA, Caffeine, Spring Security OAuth2 Client + JWT
- 버전은 `gradle/libs.versions.toml`에서만 관리한다
- 기본 패키지: `com.example.blog`

## 명령어

- 전체 빌드(테스트·정적 분석·커버리지 포함): `./gradlew build`
- 단위 테스트: `./gradlew unitTest`
- 통합 테스트(Docker 필요, Testcontainers): `./gradlew integrationTest`
- 특정 테스트: `./gradlew :service:article:article-core:test --tests "*ArticleTest"`
- 포맷 적용: `./gradlew spotlessApply`
- 정적 분석: `./gradlew spotlessCheck checkstyleMain spotbugsMain`
- 커버리지 검증: `./gradlew jacocoTestCoverageVerification`
- 로컬 DB: `docker compose up -d mysql`
- 앱 실행: `./gradlew :mains:monolith-main:bootRun --args='--spring.profiles.active=local'`

## 모듈 구조

- `libs/`: core-kernel(AggregateRoot, DomainEvent, BusinessException), rdb-kernel, web-kernel(ProblemDetail, 인증 주체), id-generator(53비트 Snowflake), events(컨텍스트 간 이벤트 계약)
- `service/{ctx}/{ctx}-core`: 패키지는 `domain`, `usecase`, `query`, `outbound`
- `service/{ctx}/adapter/{ctx}-in-{http|acl}`, `{ctx}-out-{mysql|storage|google|대상 컨텍스트}`
- `mains/monolith-main`: 조립과 설정만. 비즈니스 코드를 두지 않는다
- 컨텍스트: `article`(글·태그·검색), `comment`, `user-auth`, `media`
- `docs/`: 설계 문서(`docs/design/`), ERD·스키마 정의(`docs/data/`), API 기준 스펙(`docs/api/openapi.yaml`)

## 아키텍처 규칙 (위반 금지)

1. 의존 방향은 adapter → core → libs 하나뿐이다. core는 Spring Web, HTTP, 외부 SDK, 다른 컨텍스트 코드를 import하지 않는다.
2. 다른 컨텍스트가 필요하면 `{ctx}-out-{대상}` 어댑터가 대상의 `{대상}-in-acl` 모듈만 호출한다. `in-acl`은 core를 `implementation`으로 의존하고 자체 DTO만 노출한다.
3. 다른 컨텍스트의 core 패키지를 import하지 않는다. ArchUnit 테스트가 검사하며, 테스트를 고쳐서 통과시키지 않는다.
4. 이벤트 클래스는 `libs/events`에만 정의한다. 발행하는 쪽 core도 여기서 가져다 쓴다.
5. 애그리거트끼리는 ID로만 참조한다. 다른 컨텍스트의 테이블을 직접 조회하지 않는다.
6. 상태는 애그리거트 메서드로만 바꾼다. setter를 만들지 않는다. 확인과 실행을 `canPublish()` → `publish()`처럼 짝으로 두고, 규칙 위반은 `BusinessException(ErrorCode.X)`로 던진다.
7. 도메인 이벤트는 `AggregateRoot.registerEvent()`로 애그리거트 인스턴스에 쌓는다. 유스케이스가 저장한 뒤 `pullDomainEvents()`로 꺼내 `DomainEventPublisher` 포트로 넘긴다. 이벤트를 static·전역 상태에 모으지 않는다.
8. `@Transactional`은 유스케이스 구현(Interactor)에만 붙인다.

## 코드 컨벤션

- 유스케이스: 인터페이스 `I{동사}{대상}` 안에 `record Command`, `record Info`를 중첩한다. 구현은 `{대상}UseCasesInteractor`.
- 조회 전용: `{대상}Queries` (core의 `query` 패키지)
- outbound 포트는 역할 이름(`ArticleRepository`, `ArticleReader`), 어댑터는 `{포트}{기술}Adapter`
- 도메인 이벤트: 대상 + 과거분사(`ArticlePublished`), `record`, 공통 필드 `eventId`, `occurredAt`, `version`
- VO, DTO, Command/Info는 `record`. JPA 엔티티는 class + `protected` 기본 생성자
- Lombok은 쓰지 않는다
- ID는 `IdGenerator` 포트로 만든다. auto increment를 쓰지 않는다
- 시간은 `Clock`을 주입받아 `Instant`(UTC)로 다룬다. `LocalDateTime.now()`를 직접 호출하지 않는다
- 없을 수 있는 조회 결과는 `Optional`로 돌려준다. 도메인 메서드는 `null`을 반환하지 않는다
- 예외를 HTTP로 바꾸는 일은 `web-kernel`의 전역 핸들러만 한다. 컨트롤러에서 try-catch로 응답을 만들지 않는다

## API 규칙

- 기준 문서는 `docs/api/openapi.yaml`이다. 엔드포인트를 추가하거나 바꿀 때는 이 파일을 먼저 고친다.
- 프론트 레포는 `openapi.yaml`의 사본을 쓴다. API를 바꾼 PR에는 프론트 동기화가 필요하다고 설명에 적는다.
- 백엔드가 먼저 배포되므로 API 변경은 하위 호환으로 낸다. 이전 프론트가 그대로 동작해야 한다.
- 경로는 `/api/v1`, 관리자 API는 `/api/v1/admin`이다. 상태 변경은 `POST .../publish`처럼 명령 엔드포인트로 만든다.
- 에러는 RFC 9457 Problem Details에 확장 필드 `code`와 `requestId`를 넣는다. 새 에러 코드는 `ErrorCode` enum과 `openapi.yaml`에 함께 추가한다.
- 목록은 커서 페이지네이션이다: 요청 `cursor`, `size`(기본 10, 최대 50) → 응답 `items`, `nextCursor`. offset은 쓰지 않는다.
- 공개 API는 초안 내용과 `version`을 내보내지 않는다. DRAFT·DELETED 글은 404로 숨긴다.
- 일시는 ISO 8601 UTC로 보낸다. 값이 없으면 필드를 생략하지 않고 `null`로 보낸다.
- 도메인 규칙 위반은 409, 입력 검증 실패는 400(`errors`에 필드별 사유)이다.

## DB와 마이그레이션

- 컨텍스트마다 스키마가 따로 있다: `blog_article`, `blog_comment`, `blog_auth`, `blog_media`. 스키마를 넘는 FK는 만들지 않는다.
- Flyway 파일 위치: `service/{ctx}/adapter/{ctx}-out-mysql/src/main/resources/db/migration/{ctx}/V{n}__{설명}.sql`
- 이미 머지된 마이그레이션 파일은 고치지 않는다. 항상 새 버전 파일을 추가한다.
- 스키마 변경은 직전 이미지로 롤백해도 동작하게 한다. 컬럼 삭제·이름 변경은 확장 → 전환 → 축소 순서로 PR을 나눈다.
- JPA `ddl-auto`는 `validate`다. 스키마를 엔티티로 만들지 않는다.

## 개발 산출물 동기화

- 기능 구현 전 관련 설계·API·데이터 문서를 확인하고, 요구사항·계약·데이터 구조가 바뀌면 구현과 같은 작업에서 문서를 갱신한다.
- API 변경은 `docs/api/openapi.yaml`을 정본으로 먼저 수정하고 `docs/design/api-spec.md`, `ErrorCode`/오류 코드 목록을 맞춘다. 프론트엔드 작업이 필요한지 명시하고, 동기화 시 프론트 사본·생성 타입·연동 문서도 갱신한다.
- 테이블·컬럼·키·제약·인덱스 변경은 새 Flyway 마이그레이션과 `docs/data/erd.md`, `docs/data/schema-definition.md`를 함께 갱신한다. 머지된 마이그레이션은 수정하지 않으며 문서가 마이그레이션과 다르면 실제 구조를 정정한다.
- `docs/data/`의 모델은 첫 물리 마이그레이션이 생기기 전까지 제안 초안이다. 초안의 이름·타입·인덱스를 구현 사실로 취급하지 말고, 결정이 필요한 항목은 문서의 미결 목록에 남긴다.
- 도메인 규칙·시스템 경계·구현 순서가 바뀌면 `docs/design/plan-and-design.md`와 영향받는 ADR을 갱신한다. 코드와 산출물에 영향이 없으면 작업 요약에 그 이유를 적는다.
- 메트릭·상세 접근 로그·추적·알림·대시보드 정책은 `docs/operations/observability.md`를 함께 갱신한다. 제안 설계와 실제 의존성·설정·배포 상태를 구분하고, 로그·메트릭만으로 DB/API 산출물을 불필요하게 바꾸지 않는다.

## 테스트

- 도메인: Spring 없이 순수 단위 테스트로 작성한다(JUnit 5 + AssertJ). 상태 전이는 허용되는 전이와 거부되는 전이를 모두 파라미터 테스트로 덮는다.
- 유스케이스: Mockito나 인메모리 fake 포트를 쓴다.
- 어댑터·API: `@Tag("integration")`을 붙이고, 공유 Testcontainers 베이스 클래스를 상속한다. 컨테이너를 테스트마다 새로 띄우지 않는다.
- 테스트 설명은 `@DisplayName("제목이 비어 있으면 발행할 수 없다")`처럼 한국어로 쓴다.
- core 모듈의 라인·브랜치 커버리지가 80% 미만이면 빌드가 실패한다. 측정 제외는 `coverage-conventions.gradle.kts`에만, 사유 주석과 함께 추가한다.
- 버그를 고칠 때는 실패하는 테스트를 먼저 작성한다.

## 작업 방식

- 두 개 이상의 모듈을 건드리는 변경은 먼저 계획을 보여주고 승인을 받은 뒤 구현한다.
- 작업을 마치면 아래 "작업 후 커밋 순서"를 따른다.
- 의존성은 `libs.versions.toml`에만 추가하고, 추가하기 전에 이유를 설명한다.
- 확실하지 않은 라이브러리 API나 버전은 추측하지 말고 확인이 필요하다고 말한다.

## 브랜치와 커밋

### 브랜치

- 작업을 시작하면 먼저 최신 main에서 브랜치를 만든다: `git switch main && git pull --ff-only && git switch -c feat/article-publish`
- main에서 직접 커밋하거나 push하지 않는다
- 이름은 `{type}/{scope}-{설명}`(영어 kebab-case)이다. 예: `feat/article-publish`, `fix/comment-depth-check`, `chore/gradle-deps`
- 브랜치 하나에는 로드맵 체크리스트 항목 하나만 담는다. 범위 밖의 수정이 필요해지면 멈추고 보고한다

### 작업 후 커밋 순서

1. `./gradlew spotlessApply` 후 `./gradlew build`가 통과하는지 확인한다. 실패하면 커밋하지 않고 원인과 함께 보고한다
2. `git status`와 `git diff`로 의도하지 않은 변경(디버그 출력, 임시 파일, 시크릿, 주석 처리한 코드)이 없는지 확인한다
3. 파일을 지정해서 stage한다. `git add -A`, `git add .`은 쓰지 않는다
4. 논리 단위로 나눠 커밋한다(예: 도메인과 테스트 → 마이그레이션 → 어댑터). 커밋마다 빌드가 통과하는 상태여야 한다
5. `git log --oneline main..HEAD`로 만든 커밋 목록을 보여주고 멈춘다. push와 PR 생성은 내가 요청할 때만 한다

### 커밋 메시지

- 형식은 `type(scope): 요약`이다. 요약은 한국어 50자 이내이고 끝에 마침표를 찍지 않는다. 예: `feat(article): 초안 저장 시 버전 충돌 처리`
- 본문에는 무엇을 왜 바꿨는지 쓴다. 어떻게 바꿨는지는 코드가 보여준다
- type: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `ci`, `chore`, `perf`
- scope: `article`, `comment`, `user-auth`, `media`, `kernel`, `api`, `build`, `infra`
- `openapi.yaml`을 바꾼 커밋은 scope를 `api`로 하고 본문에 바뀐 엔드포인트를 적는다. 호환이 깨지면 꼬리말에 `BREAKING CHANGE:`를 쓴다

### PR과 머지

- PR 제목은 커밋 메시지 형식으로 쓰고, PR 템플릿의 체크리스트를 채운다
- main이 앞서 있으면 `git rebase main`으로 맞춘다. 이미 push한 브랜치는 `git push --force-with-lease`로만 갱신한다
- main에는 squash merge로 합치고, 머지한 브랜치는 지운다
- 금지: `--no-verify`로 훅 건너뛰기, `git push --force`, 이미 push한 커밋 amend, 묻지 않고 `git reset --hard`·`git clean` 실행

## 하지 말 것

- 시크릿, `.env`, 실제 토큰을 커밋하거나 코드·테스트에 하드코딩하기
- `@Disabled`나 테스트 삭제로 빌드 통과시키기
- 커버리지 제외 목록이나 ArchUnit 규칙을 고쳐서 게이트 통과시키기
- 설계 문서에 없는 인프라(Kafka, Redis, Elasticsearch 등)를 묻지 않고 추가하기
- 프론트 레포(`../frontend`)의 파일을 수정하기

## 참고 문서 (필요할 때 읽는다)

- `docs/design/plan-and-design.md`: 요구사항, 기술 결정, 도메인 모델, 데이터 모델, 이벤트, 로드맵
- `docs/design/api-spec.md`: API 공통 규약, 스키마, 에러 코드 카탈로그, 인증 흐름
- `docs/api/openapi.yaml`: API 기준 스펙
- `docs/data/README.md`: 데이터 산출물의 정본과 갱신 규칙
- `docs/data/erd.md`, `docs/data/schema-definition.md`: 논리 ERD와 제안 스키마 정의
- `docs/operations/observability.md`: 메트릭, 접근 로그, 추적 및 운영 관측성 설계
