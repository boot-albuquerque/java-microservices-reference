package com.example.payment.domain.port;

import java.util.UUID;

public interface RateLimiter {

  RateLimitResult tryAcquire(UUID userId);

  record RateLimitResult(boolean allowed, long remaining, long limit) {}
}
