# 논리 스키마 정의서

> 상태: 구현 전 제안 초안 · 기준: OpenAPI 3.1, MySQL 8.4 결정(ADR 0002) · 2026-11-03

이 문서는 초기 데이터 모델의 컬럼과 제약을 구체화하기 위한 설계 초안이다. 현재 저장소에는 DB 이름과 문자셋을 만드는 `docker/mysql/init/01-schemas.sql`만 있고 테이블 마이그레이션은 없다. 따라서 아래 타입·테이블명·인덱스는 실제 배포 스키마가 아니다. 물리 구조는 Flyway 마이그레이션으로 확정하고 그 변경에 맞춰 이 문서를 갱신한다.

## 공통 규칙

- 스키마는 `blog_article`, `blog_comment`, `blog_auth`, `blog_media`로 나눈다. 컨텍스트 간 FK는 만들지 않는다.
- ID는 OpenAPI의 53비트 안전 범위를 만족하는 Snowflake 식별자다. 저장 타입은 `BIGINT`를 제안한다.
- 일시는 UTC로 다룬다. 저장 타입 `DATETIME(6)` 제안은 구현 시 JDBC/DB 시간대 설정과 함께 검증한다.
- 문자열 기본 문자셋은 초기화 SQL의 `utf8mb4`를 따른다.
- `NULL` 여부와 타입은 초기 제안이다. 명시적 API 제약은 반영하고, API가 길이를 제한하지 않는 필드는 물리 길이를 별도 결정한다.

## `blog_article`

### `article`

| 컬럼         | 제안 타입     | NULL   | 키/규칙                         | 의미                  |
| ------------ | ------------- | ------ | ------------------------------- | --------------------- |
| `id`         | `BIGINT`      | 아니오 | PK, Snowflake                   | 글 식별자             |
| `status`     | `VARCHAR(16)` | 아니오 | `DRAFT`, `PUBLISHED`, `DELETED` | 현재 글 상태          |
| `version`    | `BIGINT`      | 아니오 | 0 이상                          | 초안 낙관적 잠금 버전 |
| `created_at` | `DATETIME(6)` | 아니오 | UTC                             | 생성 시각             |
| `updated_at` | `DATETIME(6)` | 아니오 | UTC                             | 마지막 관리 변경 시각 |

`publishedAt`은 공개/관리 API에 있으나 공개본이 마지막 발행 시각을 보유하도록 제안한다. 운영 목록에서 조인 비용이 문제가 될 경우 `article`에 중복 보관할지는 이후 측정 후 결정한다.

### `article_draft`

| 컬럼            | 제안 타입      | NULL   | 키/규칙                           | 의미                     |
| --------------- | -------------- | ------ | --------------------------------- | ------------------------ |
| `article_id`    | `BIGINT`       | 아니오 | PK, `article.id`와 동일 스키마 FK | 글 식별자 겸 초안 소유자 |
| `title`         | `VARCHAR(200)` | 아니오 | API 최대 200자                    | 현재 초안 제목           |
| `contents`      | `MEDIUMTEXT`   | 아니오 | API 최대 100,000자                | Markdown 초안            |
| `thumbnail_url` | `TEXT`         | 예     | URI 길이 제한 미정                | 초안 썸네일 주소         |

빈 초안 생성 API에 맞춰 제목·본문이 비어 있을 수 있다.

### `article_published`

| 컬럼            | 제안 타입      | NULL   | 키/규칙                           | 의미                          |
| --------------- | -------------- | ------ | --------------------------------- | ----------------------------- |
| `article_id`    | `BIGINT`       | 아니오 | PK, `article.id`와 동일 스키마 FK | 글 식별자 겸 공개본 소유자    |
| `title`         | `VARCHAR(200)` | 아니오 | 발행 시 1자 이상                  | 공개 제목                     |
| `contents`      | `MEDIUMTEXT`   | 아니오 | 발행 시 1자 이상                  | 공개 Markdown 스냅샷          |
| `thumbnail_url` | `TEXT`         | 예     | URI 길이 제한 미정                | 공개 썸네일 주소              |
| `published_at`  | `DATETIME(6)`  | 아니오 | UTC                               | 현재 공개본 발행 시각         |
| `updated_at`    | `DATETIME(6)`  | 아니오 | UTC                               | 공개본이 마지막으로 바뀐 시각 |

초안 저장만으로 공개본을 변경하지 않는다. 공개 상세의 `updatedAt`은 공개본 시각으로 제공한다는 제안이며, 정확한 의미는 API/제품 규칙으로 확정한다.

### `tag`, `article_tag`

| 테이블.컬럼              | 제안 타입     | NULL   | 키/규칙                    | 의미      |
| ------------------------ | ------------- | ------ | -------------------------- | --------- |
| `tag.id`                 | `BIGINT`      | 아니오 | PK, Snowflake              | 태그 ID   |
| `tag.name`               | `VARCHAR(30)` | 아니오 | API 1~30자                 | 태그 이름 |
| `article_tag.article_id` | `BIGINT`      | 아니오 | PK 일부, FK → `article.id` | 글 ID     |
| `article_tag.tag_id`     | `BIGINT`      | 아니오 | PK 일부, FK → `tag.id`     | 태그 ID   |

글과 태그는 다대다다. 태그 전체 교체 시 없는 이름을 생성한다. 같은 이름의 중복 허용 여부, 대소문자·공백 정규화, DB collation 아래의 UNIQUE 의미는 제품 규칙으로 확정한다. 공개 정렬은 발행일 기준이므로 인덱스 계획에 `article_tag`로 나열 순서를 보존하지 않는다.

## `blog_comment`

### `comment`

| 컬럼         | 제안 타입       | NULL   | 키/규칙             | 의미                                      |
| ------------ | --------------- | ------ | ------------------- | ----------------------------------------- |
| `id`         | `BIGINT`        | 아니오 | PK, Snowflake       | 댓글 ID                                   |
| `article_id` | `BIGINT`        | 아니오 | FK 없음, 값 참조    | 글 ID                                     |
| `author_id`  | `BIGINT`        | 아니오 | FK 없음, 값 참조    | 작성자 ID                                 |
| `parent_id`  | `BIGINT`        | 예     | 같은 테이블 self-FK | 최상위 댓글이면 NULL, 답글이면 부모 ID    |
| `content`    | `VARCHAR(1000)` | 아니오 | API 최대 1,000자    | 댓글 본문; 삭제 시 API에 빈 문자열을 반환 |
| `status`     | `VARCHAR(16)`   | 아니오 | `ACTIVE`, `DELETED` | 댓글 상태                                 |
| `created_at` | `DATETIME(6)`   | 아니오 | UTC                 | 작성 시각                                 |

대댓글 깊이는 한 단계로 제한한다. 삭제 행을 보존하는 방식과 답글이 없는 삭제 댓글의 정리 정책은 구현 전에 확정한다. OpenAPI 댓글 목록 설명은 `HIDDEN` 제외를 언급하지만 현재 enum에는 `HIDDEN`이 없다.

## `blog_auth`

### `user_account`

| 컬럼          | 제안 타입      | NULL   | 키/규칙                       | 의미          |
| ------------- | -------------- | ------ | ----------------------------- | ------------- |
| `id`          | `BIGINT`       | 아니오 | PK, Snowflake                 | 사용자 ID     |
| `email`       | `VARCHAR(320)` | 아니오 | UNIQUE 제안; 정규화 규칙 미정 | 계정 이메일   |
| `nickname`    | `VARCHAR(100)` | 아니오 | 길이 미정                     | 표시 이름     |
| `picture_url` | `TEXT`         | 예     | URI 길이 미정                 | 프로필 이미지 |
| `role`        | `VARCHAR(16)`  | 아니오 | `ADMIN`, `USER`               | 권한          |
| `created_at`  | `DATETIME(6)`  | 아니오 | UTC                           | 생성 시각     |
| `updated_at`  | `DATETIME(6)`  | 아니오 | UTC                           | 변경 시각     |

OpenAPI는 email/nickname의 길이 상한을 정의하지 않는다. `VARCHAR` 길이와 email 대소문자 정규화는 구현 전 계약과 맞춘다.

### `oauth_identity`

| 컬럼               | 제안 타입      | NULL   | 키/규칙                                      | 의미                       |
| ------------------ | -------------- | ------ | -------------------------------------------- | -------------------------- |
| `id`               | `BIGINT`       | 아니오 | PK, Snowflake                                | 외부 식별자 연결 ID        |
| `user_id`          | `BIGINT`       | 아니오 | 같은 스키마 FK → `user_account.id`           | 사용자 ID                  |
| `provider`         | `VARCHAR(32)`  | 아니오 | 현재 `google`                                | OAuth 제공자               |
| `provider_subject` | `VARCHAR(255)` | 아니오 | UNIQUE (`provider`, `provider_subject`) 제안 | 제공자 계정의 고유 subject |
| `created_at`       | `DATETIME(6)`  | 아니오 | UTC                                          | 연결 시각                  |

provider의 정확한 최대 길이는 연동 제공자가 늘어날 때 확정한다. 사용자 이메일만으로 외부 계정을 식별하지 않는다.

### `refresh_token`

| 컬럼          | 제안 타입       | NULL   | 키/규칙                            | 의미                                  |
| ------------- | --------------- | ------ | ---------------------------------- | ------------------------------------- |
| `id`          | `BIGINT`        | 아니오 | PK, Snowflake                      | 토큰 레코드 ID                        |
| `user_id`     | `BIGINT`        | 아니오 | 같은 스키마 FK → `user_account.id` | 소유 사용자                           |
| `token_hash`  | `VARBINARY(64)` | 아니오 | UNIQUE 제안                        | 원문 refresh token 대신 보관하는 해시 |
| `issued_at`   | `DATETIME(6)`   | 아니오 | UTC                                | 발급 시각                             |
| `expires_at`  | `DATETIME(6)`   | 아니오 | UTC                                | 만료 시각; 수명은 미정                |
| `consumed_at` | `DATETIME(6)`   | 예     | UTC                                | 회전으로 사용된 시각                  |
| `revoked_at`  | `DATETIME(6)`   | 예     | UTC                                | 폐기 시각                             |

이 구조는 회전된 토큰 재사용을 탐지해 사용자 토큰을 전부 폐기하려는 제안이다. 해시 알고리즘, 만료 수명, 토큰 계열화와 보존·정리 정책은 보안 설계에서 확정한다. 토큰 원문은 DB에 저장하지 않는다.

## `blog_media`

### `media`

| 컬럼                | 제안 타입       | NULL   | 키/규칙                      | 의미                       |
| ------------------- | --------------- | ------ | ---------------------------- | -------------------------- |
| `id`                | `BIGINT`        | 아니오 | PK, Snowflake; API `mediaId` | 미디어 ID                  |
| `status`            | `VARCHAR(16)`   | 아니오 | `PENDING`, `CONFIRMED`       | 업로드 확인 상태           |
| `original_filename` | `VARCHAR(255)`  | 아니오 | API 최대 255자               | 요청 파일 이름             |
| `content_type`      | `VARCHAR(64)`   | 아니오 | PNG/JPEG/GIF/WebP            | 확인된 MIME 형식           |
| `size_bytes`        | `BIGINT`        | 아니오 | 1~10,485,760                 | 업로드 바이트 수           |
| `storage_key`       | `VARCHAR(1024)` | 아니오 | 공급자 독립 값 제안          | 업로드 객체 조회용 내부 키 |
| `public_url`        | `TEXT`          | 아니오 | URI 길이 미정                | API 공개 URL               |
| `created_at`        | `DATETIME(6)`   | 아니오 | UTC                          | 업로드 URL 발급 시각       |
| `confirmed_at`      | `DATETIME(6)`   | 예     | UTC                          | 객체 확인 시각             |

스토리지 공급자·객체 키 형식·고아 업로드 정리 주기는 미정이다. `created_by` 감사 컬럼은 ERD의 미결 제안이며 이 표에는 필수 데이터로 포함하지 않았다.

## 인덱스 제안

| 테이블              | 인덱스                                                     | 목적/상태                                                    |
| ------------------- | ---------------------------------------------------------- | ------------------------------------------------------------ |
| `article`           | `(status, updated_at, id)`                                 | 관리자 목록 커서 조회. 정렬 키는 API 구현 확인 후 확정       |
| `article_published` | `(published_at, article_id)`                               | 최신 발행 글 조회/커서                                       |
| `article_published` | FULLTEXT `(title, contents)` WITH PARSER `ngram`           | 한글 검색 결정(ADR 0002); MySQL 환경에서 migration 검증 필요 |
| `tag`               | UNIQUE `(name)`                                            | 이름 정규화 결정 후 확정                                     |
| `article_tag`       | PK `(article_id, tag_id)`, 보조 `(tag_id, article_id)`     | 중복 연결 방지, 태그별 글 찾기                               |
| `comment`           | `(article_id, parent_id, created_at, id)`                  | 공개 댓글 목록과 답글 조회                                   |
| `comment`           | `(author_id, created_at)`                                  | 내 댓글/삭제 권한 조회 지원                                  |
| `oauth_identity`    | UNIQUE `(provider, provider_subject)`                      | 외부 계정 중복 연결 방지                                     |
| `refresh_token`     | UNIQUE `(token_hash)`, `(user_id, revoked_at, expires_at)` | 재사용 탐지, 사용자 토큰 폐기                                |

인덱스는 쿼리와 실행 계획이 생긴 뒤 확정한다. 실제 인덱스는 Flyway DDL과 함께 integration test/쿼리 계획으로 검증한다.

## 미결 항목

- 실제 테이블·컬럼 이름, 시간 저장 타입/세션 시간대, API의 `updatedAt` 의미
- 태그 이름의 대소문자·공백 정규화와 UNIQUE 범위
- `HIDDEN` 댓글 상태와 삭제 댓글 행 보존 정책
- 최초 ADMIN 부여, 사용자 email/nickname 최대 길이 및 정규화
- refresh token 만료 수명, 회전 계열 추적, 해시·보존·정리 정책
- 미디어 저장소, object key/URL 길이, 생성자 감사와 고아 객체 삭제
- 본문 이미지가 있는 Markdown의 미디어 FK/참조 검증 방식
