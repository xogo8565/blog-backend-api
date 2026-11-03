package com.example.blog.config;

import com.example.blog.id.IdGenerator;
import com.example.blog.id.SnowflakeIdGenerator;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 모든 컨텍스트가 함께 쓰는 Clock과 IdGenerator를 등록한다. */
@Configuration(proxyBeanMethods = false)
class CoreBeansConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  IdGenerator idGenerator(@Value("${blog.id.worker-id}") long workerId, Clock clock) {
    return new SnowflakeIdGenerator(workerId, clock, SnowflakeIdGenerator.DEFAULT_EPOCH);
  }
}
