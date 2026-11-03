# 백엔드 API 계약 요약

이 문서는 프론트엔드와 백엔드 개발자가 공통 API 규약을 빠르게 찾기 위한 안내다. 요청·응답의 기계 판독 가능한 정본은 `../api/openapi.yaml`이며, 이 문서와 다르면 OpenAPI를 우선한다.

## 공통 형식

- Base path: `/api/v1`
- 관리자 API: `/api/v1/admin`
- JSON 필드: camelCase
- Content-Type: JSON 요청·응답은 `application/json; charset=UTF-8`
- 오류 응답: RFC 9457 `application/problem+json`
- 날짜/시간: UTC ISO 8601 문자열
- ID: JSON number로 표현한 53비트 정수
- 빈 선택 값은 필드 생략 대신 `null`, 빈 목록은 `[]`
- 모든 응답에는 `X-Request-Id`가 있다. 요청에서 보낸 값을 유지하며 오류 문의에서 이 값을 사용한다.

## 목록·커서

- 페이지 요청은 `cursor`와 `size`를 받는다. 기본 `size`는 10, 최대는 50이다.
- 다음 페이지가 있으면 응답 `nextCursor`를 다음 요청의 `cursor`로 그대로 전달한다. 클라이언트는 커서 내용을 해석하거나 생성하지 않는다.
- 목록 응답은 `{ items, nextCursor }` 형태다. 마지막 페이지의 `nextCursor`는 `null`이다.
- `INVALID_CURSOR`는 400으로 응답한다. 클라이언트는 첫 페이지부터 다시 요청할 수 있게 안내한다.

## 경로 목록

| 영역 | 메서드·경로 | 접근 | 설명 |
| --- | --- | --- | --- |
| 글 | `GET /articles` | 공개 | 발행 글 목록, 태그 필터 지원 |
| 글 | `GET /articles/search` | 공개 | 제목·본문 검색, `q` 2~50자 |
| 글 | `GET /articles/{articleId}` | 공개 | 발행 글 상세. DRAFT·DELETED는 404 |
| 태그 | `GET /tags/top` | 공개 | 발행 글 수 기준 인기 태그 |
| 댓글 | `GET /articles/{articleId}/comments` | 공개 | 글 댓글 목록 |
| 댓글 | `POST /articles/{articleId}/comments` | Bearer | 댓글 또는 한 단계 답글 작성 |
| 댓글 | `DELETE /comments/{commentId}` | Bearer | 작성자 또는 ADMIN 삭제 |
| 관리자 글 | `GET /admin/articles` | ADMIN | 전체 상태 글 목록 |
| 관리자 글 | `POST /admin/articles` | ADMIN | 빈 초안 생성 |
| 관리자 글 | `GET /admin/articles/{articleId}` | ADMIN | 초안·공개본 포함 상세 |
| 관리자 글 | `DELETE /admin/articles/{articleId}` | ADMIN | 소프트 삭제 |
| 관리자 글 | `PUT /admin/articles/{articleId}/draft` | ADMIN | 버전 검증이 있는 초안 저장 |
| 관리자 글 | `POST /admin/articles/{articleId}/publish` | ADMIN | 발행·재발행 |
| 관리자 글 | `POST /admin/articles/{articleId}/unpublish` | ADMIN | 발행 취소 |
| 관리자 글 | `POST /admin/articles/{articleId}/restore` | ADMIN | 삭제 복구 |
| 태그 | `PUT /admin/articles/{articleId}/tags` | ADMIN | 태그 전체 교체, 최대 10개 |
| 미디어 | `POST /admin/media/upload-urls` | ADMIN | presigned 업로드 URL 발급 |
| 미디어 | `POST /admin/media/{mediaId}/confirm` | ADMIN | 업로드 크기·객체 확인 |
| 인증 | `GET /oauth2/authorization/google` | 공개 | Google 로그인 시작. API base path 밖 |
| 인증 | `GET /login/oauth2/code/google` | OAuth callback | refresh 쿠키 설정 후 프론트 callback으로 이동 |
| 인증 | `POST /auth/refresh` | refresh 쿠키 | access 재발급 및 refresh 회전 |
| 인증 | `POST /auth/logout` | Bearer | refresh token 폐기 |
| 인증 | `GET /me` | Bearer | 로그인 사용자 정보·권한 확인 |

## 인증과 쿠키

- 보호된 API는 `Authorization: Bearer <accessToken>`을 받는다.
- Google OAuth 성공 후 refresh token을 HttpOnly 쿠키로 전달한다. access token을 URL에 넣지 않는다.
- refresh token은 사용 시 회전한다. 이미 회전된 값의 재사용은 `REFRESH_TOKEN_REUSED` 401이며, 해당 사용자의 refresh token을 모두 폐기한다.
- 미인증은 401, 인증되었으나 ADMIN 권한이 없으면 403이다.
- OAuth 허용 계정, 쿠키 Domain/SameSite, production redirect URI는 실제 도메인 확정 후 환경별로 설정한다.

## 도메인 제약

- **Article:** `DRAFT`, `PUBLISHED`, `DELETED`. 공개 API는 `PUBLISHED`만 반환한다. 초안 저장은 `version` 불일치 시 409 `ARTICLE_VERSION_CONFLICT`다.
- **Tags:** 이름은 1~30자, 글당 최대 10개, 중복 없는 배열이다.
- **Comments:** 공개 목록은 최상위 댓글을 작성순으로 반환하고 replies에 한 단계 답글을 담는다. 더 깊은 답글은 `COMMENT_DEPTH_EXCEEDED`다. 대댓글이 있는 댓글 삭제는 DELETED 상태로 자리를 보존한다.
- **Media:** 파일은 PNG, JPEG, GIF, WebP만 허용하고 최대 10MB다. presigned URL은 10분 유효하다. 업로드 후 confirm을 거쳐야 사용할 수 있다.
- **Search:** 제목과 본문을 한글 ngram 기준으로 검색하며 관련도 대신 `publishedAt` 내림차순으로 반환한다.

## Problem Details와 상태 코드

| 상황 | 상태 | 주요 코드 |
| --- | --- | --- |
| 필드 검증 오류 | 400 | `VALIDATION_FAILED` |
| 커서·검색어 오류 | 400 | `INVALID_CURSOR`, `INVALID_SEARCH_QUERY` |
| 비로그인 / 권한 없음 | 401 / 403 | `UNAUTHENTICATED`, `FORBIDDEN` |
| 공개되지 않은 글·없는 리소스 | 404 | `ARTICLE_NOT_FOUND`, `COMMENT_NOT_FOUND`, `MEDIA_NOT_FOUND` |
| 도메인 상태·버전 충돌 | 409 | `INVALID_STATE_TRANSITION`, `ARTICLE_VERSION_CONFLICT` |
| 요청 제한 | 429 | `RATE_LIMITED` |
| 서버 오류 | 500 | `INTERNAL_ERROR` |

Problem Details는 표준 필드 `type`, `title`, `status`, `detail`, `instance`와 `code`, `requestId`를 포함한다. 입력 오류는 추가 `errors` 배열에 필드별 사유를 담는다. 신규 오류 코드는 `ErrorCode` enum과 이 문서의 API 정본 OpenAPI를 함께 갱신한다.

## 하위 호환과 프론트엔드 동기화

- 프론트엔드 타입의 기준은 백엔드 `docs/api/openapi.yaml`이다.
- 계약 변경은 백엔드 OpenAPI를 먼저 고치고, 백엔드 구현·배포 후 프론트엔드에서 `pnpm sync:api`, `pnpm gen:api`를 실행한다.
- 이미 배포된 프론트엔드와 이전 응답 소비자가 동작하도록 가능한 한 필드 추가 방식으로 변경한다.
- 파라미터 의미·정렬·에러 코드 변경은 소비자 동작을 바꾸므로 별도로 공지하고 하위 호환 영향을 검토한다.
- API 사본 동기화 커밋은 프론트엔드에서 별도 커밋으로 만들고 백엔드 커밋 SHA를 기록한다.

## 확인이 필요한 계약 차이

- 댓글 목록 설명은 HIDDEN 댓글을 제외한다고 하지만 `CommentStatus` enum은 ACTIVE·DELETED만 정의한다. 숨김 상태와 운영 API를 OpenAPI에서 확정해야 한다.
- OAuth 허용 계정, refresh cookie 속성, 실제 서비스 도메인, 미디어 저장소 URL은 운영 설정 전까지 미확정이다.
