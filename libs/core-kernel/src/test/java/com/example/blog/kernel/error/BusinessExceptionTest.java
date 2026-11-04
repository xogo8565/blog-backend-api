package com.example.blog.kernel.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BusinessExceptionTest {

  @Test
  @DisplayName("메시지를 주지 않으면 에러 코드의 기본 메시지를 쓴다")
  void usesDefaultMessage() {
    BusinessException exception = new BusinessException(ErrorCode.ARTICLE_NOT_PUBLISHABLE);

    assertThat(exception.errorCode()).isEqualTo(ErrorCode.ARTICLE_NOT_PUBLISHABLE);
    assertThat(exception.getMessage()).isEqualTo("제목과 본문을 입력해야 발행할 수 있습니다.");
  }

  @Test
  @DisplayName("상세 메시지를 주면 그 메시지를 쓴다")
  void usesGivenDetail() {
    BusinessException exception =
        new BusinessException(ErrorCode.ARTICLE_VERSION_CONFLICT, "요청 version 7, 현재 version 8");

    assertThat(exception.getMessage()).isEqualTo("요청 version 7, 현재 version 8");
    assertThat(exception.errorCode().category()).isEqualTo(ErrorCode.Category.CONFLICT);
  }

  @Test
  @DisplayName("에러 코드 없이는 만들 수 없다")
  void rejectsNullErrorCode() {
    assertThatNullPointerException().isThrownBy(() -> new BusinessException(null));
    assertThatNullPointerException().isThrownBy(() -> new BusinessException(null, "detail"));
  }
}
