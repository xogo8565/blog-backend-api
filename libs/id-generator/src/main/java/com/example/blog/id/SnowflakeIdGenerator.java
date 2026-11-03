package com.example.blog.id;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * 53비트 Snowflake ID 생성기.
 *
 * <pre>
 * | 타임스탬프 41비트 (epoch 기준 ms, 약 69년) | 워커 4비트 (0~15) | 시퀀스 8비트 (ms당 256개) |
 * </pre>
 *
 * <p>최댓값이 2^53 - 1이라 JSON number로 보내도 JavaScript에서 정밀도를 잃지 않는다. 같은 밀리초에 256개를 넘게 만들면 다음 밀리초까지 기다린다.
 * 시계가 {@value #MAX_BACKWARD_MILLIS}ms 이내로 뒤로 가면 기다리고, 그보다 크면 예외를 던진다.
 */
public final class SnowflakeIdGenerator implements IdGenerator {

  public static final Instant DEFAULT_EPOCH = Instant.parse("2026-01-01T00:00:00Z");
  public static final long MAX_ID = (1L << 53) - 1;
  public static final long MAX_WORKER_ID = (1L << 4) - 1;
  static final long MAX_BACKWARD_MILLIS = 10;

  private static final int WORKER_BITS = 4;
  private static final int SEQUENCE_BITS = 8;
  private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;
  private static final long MAX_TIMESTAMP = (1L << 41) - 1;

  private final long workerId;
  private final Clock clock;
  private final long epochMillis;

  private long lastTimestamp = -1L;
  private long sequence;

  public SnowflakeIdGenerator(long workerId) {
    this(workerId, Clock.systemUTC(), DEFAULT_EPOCH);
  }

  public SnowflakeIdGenerator(long workerId, Clock clock, Instant epoch) {
    if (workerId < 0 || workerId > MAX_WORKER_ID) {
      throw new IllegalArgumentException("workerId는 0~" + MAX_WORKER_ID + " 범위여야 합니다: " + workerId);
    }
    this.workerId = workerId;
    this.clock = Objects.requireNonNull(clock, "clock");
    this.epochMillis = Objects.requireNonNull(epoch, "epoch").toEpochMilli();
  }

  @Override
  public synchronized long nextId() {
    long timestamp = currentTimestamp();
    if (timestamp < lastTimestamp) {
      long drift = lastTimestamp - timestamp;
      if (drift > MAX_BACKWARD_MILLIS) {
        throw new IllegalStateException("시계가 " + drift + "ms 뒤로 갔습니다. ID를 만들 수 없습니다.");
      }
      timestamp = waitUntil(lastTimestamp);
    }

    if (timestamp == lastTimestamp) {
      sequence = (sequence + 1) & MAX_SEQUENCE;
      if (sequence == 0) {
        timestamp = waitUntil(lastTimestamp + 1);
      }
    } else {
      sequence = 0;
    }

    if (timestamp < 0 || timestamp > MAX_TIMESTAMP) {
      throw new IllegalStateException("타임스탬프가 41비트 범위를 벗어났습니다: " + timestamp);
    }
    lastTimestamp = timestamp;
    return (timestamp << (WORKER_BITS + SEQUENCE_BITS)) | (workerId << SEQUENCE_BITS) | sequence;
  }

  /** ID에 담긴 타임스탬프(epoch 기준 ms). */
  public static long timestampOf(long id) {
    return id >>> (WORKER_BITS + SEQUENCE_BITS);
  }

  /** ID에 담긴 워커 번호. */
  public static long workerOf(long id) {
    return (id >>> SEQUENCE_BITS) & MAX_WORKER_ID;
  }

  /** ID에 담긴 시퀀스. */
  public static long sequenceOf(long id) {
    return id & MAX_SEQUENCE;
  }

  private long currentTimestamp() {
    return clock.millis() - epochMillis;
  }

  private long waitUntil(long target) {
    long timestamp = currentTimestamp();
    while (timestamp < target) {
      Thread.onSpinWait();
      timestamp = currentTimestamp();
    }
    return timestamp;
  }
}
