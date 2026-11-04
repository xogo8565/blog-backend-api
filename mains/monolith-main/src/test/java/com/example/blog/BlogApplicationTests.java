package com.example.blog;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.blog.id.IdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BlogApplicationTests {

  @Autowired private IdGenerator idGenerator;

  @Test
  @DisplayName("애플리케이션 컨텍스트가 뜨고 ID 생성기가 동작한다")
  void contextLoads() {
    assertThat(idGenerator.nextId()).isPositive();
  }
}
