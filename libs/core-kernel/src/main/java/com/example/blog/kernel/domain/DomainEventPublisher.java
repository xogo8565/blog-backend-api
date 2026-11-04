package com.example.blog.kernel.domain;

import java.util.Collection;

/**
 * 도메인 이벤트 발행 포트.
 *
 * <p>P2에서는 커밋 후 인프로세스 이벤트로, P5에서는 transactional outbox로 구현을 바꾼다. core 코드는 이 포트만 안다.
 */
public interface DomainEventPublisher {

  void publish(DomainEvent event);

  default void publishAll(Collection<? extends DomainEvent> events) {
    events.forEach(this::publish);
  }
}
