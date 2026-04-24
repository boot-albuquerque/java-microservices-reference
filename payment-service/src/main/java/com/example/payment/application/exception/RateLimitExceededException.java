package com.example.payment.application.exception;

import com.example.payment.domain.exception.DomainException;
import java.util.UUID;

public class RateLimitExceededException extends DomainException {

  public RateLimitExceededException(UUID userId, long retryAfterMs) {
    super("Rate limit exceeded for user " + userId + ". Retry after " + retryAfterMs + "ms");
  }
}
