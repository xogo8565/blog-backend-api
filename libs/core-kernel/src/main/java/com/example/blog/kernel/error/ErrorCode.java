package com.example.blog.kernel.error;

/**
 * API 에러 코드 카탈로그. docs/api/openapi.yaml의 ErrorCode와 항상 같아야 한다(ErrorCodeSpecTest가 검사).
 *
 * <p>HTTP 상태로 바꾸는 일은 web-kernel이 {@link Category}를 보고 한다. core는 HTTP를 모른다.
 */
public enum ErrorCode {
  VALIDATION_FAILED(Category.INVALID_INPUT, "요청 값이 올바르지 않습니다."),
  INVALID_CURSOR(Category.INVALID_INPUT, "커서 값이 올바르지 않습니다."),
  INVALID_SEARCH_QUERY(Category.INVALID_INPUT, "검색어는 2~50자여야 합니다."),
  UNSUPPORTED_MEDIA_TYPE(Category.INVALID_INPUT, "지원하지 않는 이미지 형식입니다."),
  MEDIA_TOO_LARGE(Category.INVALID_INPUT, "이미지는 10MB 이하만 올릴 수 있습니다."),
  UNAUTHENTICATED(Category.UNAUTHENTICATED, "로그인이 필요합니다."),
  REFRESH_TOKEN_INVALID(Category.UNAUTHENTICATED, "다시 로그인해 주세요."),
  REFRESH_TOKEN_REUSED(Category.UNAUTHENTICATED, "보안을 위해 다시 로그인해 주세요."),
  FORBIDDEN(Category.FORBIDDEN, "권한이 없습니다."),
  COMMENT_FORBIDDEN(Category.FORBIDDEN, "다른 사람의 댓글은 삭제할 수 없습니다."),
  ARTICLE_NOT_FOUND(Category.NOT_FOUND, "글을 찾을 수 없습니다."),
  COMMENT_NOT_FOUND(Category.NOT_FOUND, "댓글을 찾을 수 없습니다."),
  MEDIA_NOT_FOUND(Category.NOT_FOUND, "이미지를 찾을 수 없습니다."),
  CATEGORY_NOT_FOUND(Category.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
  ARTICLE_NOT_PUBLISHABLE(Category.CONFLICT, "제목과 본문을 입력해야 발행할 수 있습니다."),
  INVALID_STATE_TRANSITION(Category.CONFLICT, "현재 상태에서는 할 수 없는 작업입니다."),
  ARTICLE_VERSION_CONFLICT(Category.CONFLICT, "초안이 다른 곳에서 먼저 저장되었습니다."),
  ARTICLE_DELETED(Category.CONFLICT, "삭제된 글은 수정할 수 없습니다."),
  COMMENT_DEPTH_EXCEEDED(Category.CONFLICT, "대댓글에는 답글을 달 수 없습니다."),
  MEDIA_NOT_UPLOADED(Category.CONFLICT, "업로드된 파일을 찾을 수 없습니다."),
  CATEGORY_SLUG_DUPLICATED(Category.CONFLICT, "이미 쓰고 있는 카테고리 슬러그입니다."),
  CATEGORY_NOT_EMPTY(Category.CONFLICT, "하위 카테고리나 글이 있는 카테고리는 삭제할 수 없습니다."),
  CATEGORY_DEPTH_EXCEEDED(Category.CONFLICT, "카테고리는 2단계까지만 만들 수 있습니다."),
  RATE_LIMITED(Category.RATE_LIMITED, "잠시 후 다시 시도해 주세요."),
  INTERNAL_ERROR(Category.INTERNAL, "잠시 후 다시 시도해 주세요.");

  /** 에러의 종류. web-kernel이 HTTP 상태를 정할 때 쓴다. */
  public enum Category {
    INVALID_INPUT,
    UNAUTHENTICATED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    RATE_LIMITED,
    INTERNAL
  }

  private final Category category;
  private final String defaultMessage;

  ErrorCode(Category category, String defaultMessage) {
    this.category = category;
    this.defaultMessage = defaultMessage;
  }

  public Category category() {
    return category;
  }

  public String defaultMessage() {
    return defaultMessage;
  }
}
