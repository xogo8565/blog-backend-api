package com.example.blog.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SnowflakeIdGeneratorTest {

  private static final Instant EPOCH = SnowflakeIdGenerator.DEFAULT_EPOCH;

  /** 직접 시각을 정하고, 정해진 횟수만큼 읽히면 1ms씩 앞으로 가는 테스트용 시계. */
  static final class SteppingClock extends Clock {

    private long millis;
    private final int readsPerStep;
    private int reads;

    SteppingClock(long millis, int readsPerStep) {
      this.millis = millis;
      this.readsPerStep = readsPerStep;
    }

    void set(long millis) {
      this.millis = millis;
      this.reads = 0;
    }

    @Override
    public long millis() {
      reads++;
      if (readsPerStep > 0 && reads > readsPerStep) {
        millis++;
        reads = 1;
      }
      return millis;
    }

    @Override
    public Instant instant() {
      return Instant.ofEpochMilli(millis());
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }
  }

  @Test
  @DisplayName("ID는 타임스탬프, 워커, 시퀀스로 분해된다")
  void encodesTimestampWorkerAndSequence() {
    SteppingClock clock = new SteppingClock(EPOCH.toEpochMilli() + 1_000, 0);
    SnowflakeIdGenerator generator = new SnowflakeIdGenerator(7, clock, EPOCH);

    long first = generator.nextId();
    long second = generator.nextId();

    assertThat(SnowflakeIdGenerator.timestampOf(first)).isEqualTo(1_000);
    assertThat(SnowflakeIdGenerator.workerOf(first)).isEqualTo(7);
    assertThat(SnowflakeIdGenerator.sequenceOf(first)).isZero();
    assertThat(SnowflakeIdGenerator.sequenceOf(second)).isEqualTo(1);
  }

  @Test
  @DisplayName("타임스탬프가 최댓값이어도 ID는 2^53 - 1을 넘지 않는다")
  void neverExceeds53Bits() {
    long maxTimestamp = (1L << 41) - 1;
    SteppingClock clock = new SteppingClock(EPOCH.toEpochMilli() + maxTimestamp, 0);
    SnowflakeIdGenerator generator =
        new SnowflakeIdGenerator(SnowflakeIdGenerator.MAX_WORKER_ID, clock, EPOCH);

    long id = generator.nextId();

    assertThat(id).isPositive().isLessThanOrEqualTo(SnowflakeIdGenerator.MAX_ID);
  }

  @Test
  @DisplayName("타임스탬프가 41비트를 넘으면 예외를 던진다")
  void failsWhenTimestampOverflows() {
    SteppingClock clock = new SteppingClock(EPOCH.toEpochMilli() + (1L << 41), 0);
    SnowflakeIdGenerator generator = new SnowflakeIdGenerator(0, clock, EPOCH);

    assertThatIllegalStateException().isThrownBy(generator::nextId);
  }

  @Test
  @DisplayName("epoch 이전 시각이면 예외를 던진다")
  void failsBeforeEpoch() {
    SteppingClock clock = new SteppingClock(EPOCH.toEpochMilli() - 1, 0);
    SnowflakeIdGenerator generator = new SnowflakeIdGenerator(0, clock, EPOCH);

    assertThatIllegalStateException().isThrownBy(generator::nextId);
  }

  @Test
  @DisplayName("한 밀리초에 256개를 넘게 만들면 다음 밀리초로 넘어간다")
  void waitsForNextMillisWhenSequenceOverflows() {
    SteppingClock clock = new SteppingClock(EPOCH.toEpochMilli() + 10, 300);
    SnowflakeIdGenerator generator = new SnowflakeIdGenerator(0, clock, EPOCH);

    List<Long> ids = new ArrayList<>();
    for (int i = 0; i < 257; i++) {
      ids.add(generator.nextId());
    }

    long last = ids.get(256);
    assertThat(SnowflakeIdGenerator.timestampOf(ids.get(255))).isEqualTo(10);
    assertThat(SnowflakeIdGenerator.timestampOf(last)).isEqualTo(11);
    assertThat(SnowflakeIdGenerator.sequenceOf(last)).isZero();
    assertThat(ids).doesNotHaveDuplicates().isSorted();
  }

  @Test
  @DisplayName("시계가 조금 뒤로 가면 따라잡을 때까지 기다린다")
  void waitsWhenClockMovesSlightlyBackwards() {
    SteppingClock clock = new SteppingClock(EPOCH.toEpochMilli() + 100, 1);
    SnowflakeIdGenerator generator = new SnowflakeIdGenerator(0, clock, EPOCH);
    long before = generator.nextId();

    clock.set(EPOCH.toEpochMilli() + 95);
    long after = generator.nextId();

    assertThat(after).isGreaterThan(before);
    assertThat(SnowflakeIdGenerator.timestampOf(after)).isGreaterThanOrEqualTo(100);
  }

  @Test
  @DisplayName("시계가 10ms보다 많이 뒤로 가면 예외를 던진다")
  void failsWhenClockMovesFarBackwards() {
    SteppingClock clock = new SteppingClock(EPOCH.toEpochMilli() + 1_000, 0);
    SnowflakeIdGenerator generator = new SnowflakeIdGenerator(0, clock, EPOCH);
    generator.nextId();

    clock.set(EPOCH.toEpochMilli() + 900);

    assertThatIllegalStateException().isThrownBy(generator::nextId);
  }

  @ParameterizedTest
  @ValueSource(longs = {-1, 16})
  @DisplayName("workerId가 0~15 범위를 벗어나면 만들 수 없다")
  void rejectsInvalidWorkerId(long workerId) {
    assertThatIllegalArgumentException().isThrownBy(() -> new SnowflakeIdGenerator(workerId));
  }

  @Test
  @DisplayName("여러 스레드가 동시에 만들어도 중복이 없다")
  void generatesUniqueIdsConcurrently() throws Exception {
    SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1);
    Set<Long> ids = ConcurrentHashMap.newKeySet();
    int threads = 8;
    int perThread = 5_000;

    try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
      List<Future<?>> futures = new ArrayList<>();
      for (int t = 0; t < threads; t++) {
        futures.add(
            executor.submit(
                () -> {
                  for (int i = 0; i < perThread; i++) {
                    ids.add(generator.nextId());
                  }
                }));
      }
      for (Future<?> future : futures) {
        future.get();
      }
    }

    assertThat(ids).hasSize(threads * perThread);
  }

  @Test
  @DisplayName("한 스레드에서 만든 ID는 계속 증가한다")
  void increasesMonotonically() {
    SnowflakeIdGenerator generator = new SnowflakeIdGenerator(2);
    Set<Long> seen = new HashSet<>();
    long previous = -1;
    for (int i = 0; i < 10_000; i++) {
      long id = generator.nextId();
      assertThat(id).isGreaterThan(previous);
      assertThat(seen.add(id)).isTrue();
      previous = id;
    }
  }
}
