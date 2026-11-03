# 데이터 모델 산출물

- [`erd.md`](erd.md): 바운디드 컨텍스트별 논리 엔티티와 관계
- [`schema-definition.md`](schema-definition.md): 테이블·컬럼·타입·키·인덱스 초안
- 물리 스키마 정본: `service/{context}/adapter/{context}-out-mysql/src/main/resources/db/migration/{context}/V{version}__{description}.sql`

현재 ERD와 스키마 정의는 구현 전 초안이다. API/도메인 규칙만으로 확정할 수 없는 컬럼, 길이, 인덱스, 개인정보 보존 정책은 미결 항목으로 표시한다. Flyway 마이그레이션을 추가하거나 변경하는 같은 작업에서 ERD와 스키마 정의도 갱신한다. 마이그레이션과 문서가 다르면 마이그레이션을 기준으로 문서를 바로잡는다.

API의 공개·관리 요청/응답 정본은 [`../api/openapi.yaml`](../api/openapi.yaml)이다. API JSON 모델과 DB 정규화 모델은 목적이 다르므로 이 문서는 OpenAPI를 DB 테이블 구조로 그대로 복사하지 않는다.
