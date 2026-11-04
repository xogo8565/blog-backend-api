package com.example.blog.kernel.domain;

import java.time.Instant;
import java.util.UUID;

/** 도메인 이벤트의 공통 계약. 컨텍스트 간 이벤트는 libs/events에 record로 정의한다. */
public interface DomainEvent {

  /** 이벤트 식별자. 구독자가 중복 처리를 막는 데 쓴다. */
  UUID eventId();

  /** 이벤트가 발생한 시각(UTC). */
  Instant occurredAt();

  /** 이벤트 스키마 버전. 호환되지 않게 바뀌면 새 타입을 만든다. */
  int version();
}
