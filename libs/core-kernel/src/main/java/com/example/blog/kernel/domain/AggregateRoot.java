package com.example.blog.kernel.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 애그리거트 루트의 공통 부모.
 *
 * <p>상태를 바꾸는 메서드는 {@link #registerEvent(DomainEvent)}로 이벤트를 쌓고, 유스케이스는 저장한 뒤 {@link
 * #pullDomainEvents()}로 꺼내 {@link DomainEventPublisher}에 넘긴다. 이벤트 목록은 인스턴스 필드라 테스트끼리 영향을 주지 않으며,
 * {@code transient}라서 JPA가 저장하지 않는다.
 *
 * @param <I> 식별자 타입
 */
public abstract class AggregateRoot<I> {

  private final transient List<DomainEvent> domainEvents = new ArrayList<>();

  /** 애그리거트 식별자. */
  public abstract I getId();

  /** 상태 변경과 함께 발행할 이벤트를 쌓는다. */
  protected void registerEvent(DomainEvent event) {
    domainEvents.add(Objects.requireNonNull(event, "event"));
  }

  /** 쌓인 이벤트를 꺼내고 목록을 비운다. 같은 이벤트가 두 번 발행되지 않는다. */
  public List<DomainEvent> pullDomainEvents() {
    List<DomainEvent> events = List.copyOf(domainEvents);
    domainEvents.clear();
    return events;
  }
}
