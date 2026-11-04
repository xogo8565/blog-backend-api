# 설계 문서

`plan-and-design.md`는 시스템 목표·도메인·아키텍처 설계 기준이고, `api-spec.md`는 API 계약을 빠르게 찾기 위한 요약이다. 데이터 모델은 [`../data/README.md`](../data/README.md)의 ERD와 스키마 정의를 본다. 기존에 안내된 Claude Doc 원본은 이 작업공간에서 확인할 수 없어, 현재 저장소의 지침·ADR·OpenAPI를 기준으로 작성했다.

API 계약의 정본은 `../api/openapi.yaml`이다. 요약 문서와 OpenAPI가 다르면 OpenAPI를 우선한다.

메트릭·접근 로그·추적과 운영 대시보드 설계는 [`../operations/observability.md`](../operations/observability.md)를 따른다. 이 문서는 제안 상태이며, 실제 의존성·설정·수집 인프라와 구분한다.

문서 종류별 정본과 갱신 시점은 [`../README.md`](../README.md)를 따른다. DB의 물리 구조는 문서 초안이 아니라 Flyway 마이그레이션을 기준으로 한다.
