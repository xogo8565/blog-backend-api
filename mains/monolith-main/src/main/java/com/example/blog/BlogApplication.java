package com.example.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 블로그 백엔드 진입점. 컨텍스트 모듈을 조립하고 설정만 둔다. */
@SpringBootApplication
public class BlogApplication {

  public static void main(String[] args) {
    SpringApplication.run(BlogApplication.class, args);
  }
}
