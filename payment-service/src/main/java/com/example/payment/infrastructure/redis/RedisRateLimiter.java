package com.example.payment.infrastructure.redis;

import com.example.payment.domain.port.RateLimiter;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Fixed-window rate limiter: 100 requests per 60-second window per user. The window is approximated
 * by a Redis key with 60s TTL; counter increments atomically via INCR.
 */
@Component
public class RedisRateLimiter implements RateLimiter {

  private static final String PREFIX = "rate:";
  private static final long LIMIT = 100L;
  private static final Duration WINDOW = Duration.ofSeconds(60);

  private final StringRedisTemplate redis;

  public RedisRateLimiter(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public RateLimitResult tryAcquire(UUID userId) {
    String key = PREFIX + userId;
    Long count = redis.opsForValue().increment(key);
    if (count == null) {
      return new RateLimitResult(false, 0L, LIMIT);
    }
    if (count == 1L) {
      redis.expire(key, WINDOW);
    }
    long remaining = Math.max(0L, LIMIT - count);
    return new RateLimitResult(count <= LIMIT, remaining, LIMIT);
  }
}
