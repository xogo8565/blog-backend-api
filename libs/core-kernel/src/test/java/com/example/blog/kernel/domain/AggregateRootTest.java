package com.example.blog.kernel.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AggregateRootTest {

  record SampleEvent(UUID eventId, Instant occurredAt, int version) implements DomainEvent {}

  static final class SampleAggregate extends AggregateRoot<Long> {

    @Override
    public Long getId() {
      return 1L;
    }

    void doSomething(DomainEvent event) {
      registerEvent(event);
    }
  }

  private static SampleEvent newEvent() {
    return new SampleEvent(UUID.randomUUID(), Instant.EPOCH, 1);
  }

  @Test
  @DisplayName("쌓은 이벤트를 쌓은 순서대로 꺼낸다")
  void pullsEventsInOrder() {
    SampleAggregate aggregate = new SampleAggregate();
    SampleEvent first = newEvent();
    SampleEvent second = newEvent();

    aggregate.doSomething(first);
    aggregate.doSomething(second);

    assertThat(aggregate.pullDomainEvents()).containsExactly(first, second);
  }

  @Test
  @DisplayName("이벤트를 꺼내면 목록이 비어서 같은 이벤트를 두 번 발행하지 않는다")
  void clearsEventsAfterPull() {
    SampleAggregate aggregate = new SampleAggregate();
    aggregate.doSomething(newEvent());

    aggregate.pullDomainEvents();

    assertThat(aggregate.pullDomainEvents()).isEmpty();
  }

  @Test
  @DisplayName("null 이벤트는 쌓을 수 없다")
  void rejectsNullEvent() {
    SampleAggregate aggregate = new SampleAggregate();

    assertThatNullPointerException().isThrownBy(() -> aggregate.doSomething(null));
  }

  @Test
  @DisplayName("publishAll은 이벤트마다 publish를 호출한다")
  void publishAllDelegatesToPublish() {
    List<DomainEvent> published = new ArrayList<>();
    DomainEventPublisher publisher = published::add;
    SampleEvent first = newEvent();
    SampleEvent second = newEvent();

    publisher.publishAll(List.of(first, second));

    assertThat(published).containsExactly(first, second);
  }
}
