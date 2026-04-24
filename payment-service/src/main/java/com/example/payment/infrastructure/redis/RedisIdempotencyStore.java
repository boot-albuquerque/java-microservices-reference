package com.example.payment.infrastructure.redis;

import com.example.payment.domain.port.IdempotencyStore;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisIdempotencyStore implements IdempotencyStore {

  private static final String PREFIX = "idempotency:";
  private static final Duration TTL = Duration.ofHours(24);

  private final StringRedisTemplate redis;

  public RedisIdempotencyStore(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public Optional<UUID> find(UUID idempotencyKey) {
    String value = redis.opsForValue().get(PREFIX + idempotencyKey);
    return Optional.ofNullable(value).map(UUID::fromString);
  }

  @Override
  public void store(UUID idempotencyKey, UUID paymentId) {
    redis.opsForValue().set(PREFIX + idempotencyKey, paymentId.toString(), TTL);
  }

  @Override
  public boolean storeIfAbsent(UUID idempotencyKey, UUID paymentId) {
    Boolean stored =
        redis.opsForValue().setIfAbsent(PREFIX + idempotencyKey, paymentId.toString(), TTL);
    return Boolean.TRUE.equals(stored);
  }
}
