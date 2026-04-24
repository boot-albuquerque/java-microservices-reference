package com.example.payment.application.dto;

import com.example.payment.domain.model.Payment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    BigDecimal amount,
    String currency,
    String status,
    UUID payerId,
    UUID payeeId,
    String externalReference,
    Instant createdAt,
    Instant updatedAt) {

  public static PaymentResponse from(Payment payment) {
    return new PaymentResponse(
        payment.id(),
        payment.amount(),
        payment.currency(),
        payment.status().name(),
        payment.payerId(),
        payment.payeeId(),
        payment.externalReference(),
        payment.createdAt(),
        payment.updatedAt());
  }
}
