package com.example.blog.id;

/** ID 생성 포트. 모든 애그리거트의 ID는 이 포트로 만든다. auto increment는 쓰지 않는다. */
@FunctionalInterface
public interface IdGenerator {

  long nextId();
}
