package com.example.payment.application.exception;

import com.example.payment.domain.exception.DomainException;
import java.util.UUID;

public class IdempotencyConflictException extends DomainException {

  public IdempotencyConflictException(UUID idempotencyKey) {
    super("Idempotency conflict for key: " + idempotencyKey);
  }
}
