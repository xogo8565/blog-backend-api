package com.example.blog.kernel.error;

import java.util.Objects;

/** 도메인 규칙 위반을 알리는 예외. 응답 형식(Problem Details)은 web-kernel이 만든다. */
public final class BusinessException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode) {
    this(errorCode, Objects.requireNonNull(errorCode, "errorCode").defaultMessage());
  }

  public BusinessException(ErrorCode errorCode, String detail) {
    super(detail);
    this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
  }

  public ErrorCode errorCode() {
    return errorCode;
  }
}
