package com.example.payment.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.payment.AbstractIntegrationTest;
import com.example.payment.domain.port.RateLimiter;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RedisRateLimiterIT extends AbstractIntegrationTest {

  @Autowired private RateLimiter rateLimiter;

  @Test
  void shouldAllowFirstRequest() {
    UUID userId = UUID.randomUUID();
    RateLimiter.RateLimitResult result = rateLimiter.tryAcquire(userId);
    assertThat(result.allowed()).isTrue();
    assertThat(result.remaining()).isEqualTo(99L);
    assertThat(result.limit()).isEqualTo(100L);
  }

  @Test
  void shouldRejectAfter100() {
    UUID userId = UUID.randomUUID();
    for (int i = 0; i < 100; i++) {
      rateLimiter.tryAcquire(userId);
    }
    RateLimiter.RateLimitResult result = rateLimiter.tryAcquire(userId);
    assertThat(result.allowed()).isFalse();
    assertThat(result.remaining()).isEqualTo(0L);
  }
}
