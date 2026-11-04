# 개발 산출물 안내

이 디렉터리는 제품·시스템 설계, API 계약, 데이터 모델의 기준 문서를 모은다. 개발자는 변경 범위에 맞는 산출물을 확인하고, 구현과 계약·구조가 달라지면 같은 변경에서 문서를 갱신한다. 자세한 절차는 [`CLAUDE.md`](../CLAUDE.md)의 **개발 산출물 동기화**를 따른다.

| 산출물                   | 기준/정본                                                                          | 갱신 시점                                                         |
| ------------------------ | ---------------------------------------------------------------------------------- | ----------------------------------------------------------------- |
| 시스템·도메인 설계       | [`design/plan-and-design.md`](design/plan-and-design.md), 관련 ADR                 | 도메인 규칙, 서비스 경계, 주요 기술 결정이나 구현 순서가 바뀔 때  |
| API 계약                 | [`api/openapi.yaml`](api/openapi.yaml)                                             | 엔드포인트, 요청·응답, 인증, 상태 코드, 오류 코드가 바뀔 때       |
| API 읽기 안내            | [`design/api-spec.md`](design/api-spec.md)                                         | OpenAPI의 공통 규칙이나 소비자 안내가 바뀔 때                     |
| 운영·관측성 설계         | [`operations/observability.md`](operations/observability.md)                        | 메트릭, 접근 로그, 추적, 대시보드, 알림 또는 운영 수집 구성이 바뀔 때 |
| 논리 ERD                 | [`data/erd.md`](data/erd.md)                                                       | 엔티티·관계·바운디드 컨텍스트가 바뀔 때                           |
| 데이터 스키마 정의       | [`data/schema-definition.md`](data/schema-definition.md)                           | 테이블·컬럼·제약·인덱스가 바뀔 때                                 |
| 물리 DB 구조             | 컨텍스트별 Flyway 마이그레이션                                                     | DB에 실제 구조를 만들거나 변경할 때. 물리 구조의 정본             |
| 프론트엔드 화면/API 연동 | `../../frontend/docs/design/frontend.md`, `../../frontend/docs/design/api-spec.md` | 사용자 흐름, 화면 상태, 접근성, 캐시 또는 API 소비 방식이 바뀔 때 |

## 현재 산출물 상태

2026-11-03 기준으로 DB 초기화 SQL은 네 개의 MySQL 스키마만 만든다. 컨텍스트 테이블을 만드는 Flyway 마이그레이션이나 JPA 엔티티는 아직 확인되지 않았다. 따라서 `data/erd.md`와 `data/schema-definition.md`는 현재 OpenAPI와 설계 결정을 바탕으로 한 **논리 설계 초안**이다. 실제 테이블이 구현되면 Flyway 파일을 물리 정본으로 삼고 두 문서를 동기화한다.

OpenAPI에는 댓글 숨김 상태(`HIDDEN`) 설명과 `CommentStatus` enum(`ACTIVE`, `DELETED`) 사이의 불일치가 있다. 데이터 모델은 숨김 상태를 확정하지 않으며, API 계약과 함께 결정한다.
