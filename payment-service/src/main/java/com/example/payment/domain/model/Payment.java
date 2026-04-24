package com.example.payment.domain.model;

import com.example.payment.domain.event.PaymentCreated;
import com.example.payment.domain.event.PaymentProcessed;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class Payment {

  private final UUID id;
  private final BigDecimal amount;
  private final String currency;
  private final PaymentStatus status;
  private final UUID idempotencyKey;
  private final UUID payerId;
  private final UUID payeeId;
  private final String externalReference;
  private final Instant createdAt;
  private final Instant updatedAt;

  private Payment(
      UUID id,
      BigDecimal amount,
      String currency,
      PaymentStatus status,
      UUID idempotencyKey,
      UUID payerId,
      UUID payeeId,
      String externalReference,
      Instant createdAt,
      Instant updatedAt) {
    if (id == null) throw new IllegalArgumentException("id must not be null");
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
      throw new IllegalArgumentException("amount must be positive");
    if (currency == null || currency.length() != 3 || !currency.equals(currency.toUpperCase()))
      throw new IllegalArgumentException("currency must be a 3-char uppercase ISO 4217 code");
    if (idempotencyKey == null)
      throw new IllegalArgumentException("idempotencyKey must not be null");
    if (payerId == null) throw new IllegalArgumentException("payerId must not be null");
    if (payeeId == null) throw new IllegalArgumentException("payeeId must not be null");

    this.id = id;
    this.amount = amount;
    this.currency = currency;
    this.status = status;
    this.idempotencyKey = idempotencyKey;
    this.payerId = payerId;
    this.payeeId = payeeId;
    this.externalReference = externalReference;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static Payment reconstitute(
      UUID id,
      BigDecimal amount,
      String currency,
      PaymentStatus status,
      UUID idempotencyKey,
      UUID payerId,
      UUID payeeId,
      String externalReference,
      Instant createdAt,
      Instant updatedAt) {
    return new Payment(
        id,
        amount,
        currency,
        status,
        idempotencyKey,
        payerId,
        payeeId,
        externalReference,
        createdAt,
        updatedAt);
  }

  public static Payment createPending(
      UUID id,
      BigDecimal amount,
      String currency,
      UUID idempotencyKey,
      UUID payerId,
      UUID payeeId,
      String externalReference,
      Instant now) {
    return new Payment(
        id,
        amount,
        currency,
        PaymentStatus.PENDING,
        idempotencyKey,
        payerId,
        payeeId,
        externalReference,
        now,
        now);
  }

  public Payment withStatus(PaymentStatus next, Instant now) {
    PaymentStateMachine.assertValidTransition(this.status, next);
    return new Payment(
        id,
        amount,
        currency,
        next,
        idempotencyKey,
        payerId,
        payeeId,
        externalReference,
        createdAt,
        now);
  }

  public UUID id() {
    return id;
  }

  public BigDecimal amount() {
    return amount;
  }

  public String currency() {
    return currency;
  }

  public PaymentStatus status() {
    return status;
  }

  public UUID idempotencyKey() {
    return idempotencyKey;
  }

  public UUID payerId() {
    return payerId;
  }

  public UUID payeeId() {
    return payeeId;
  }

  public String externalReference() {
    return externalReference;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public PaymentCreated toCreatedEvent() {
    return new PaymentCreated(id, amount, currency, payerId, payeeId, externalReference, createdAt);
  }

  public PaymentProcessed toProcessedEvent() {
    return new PaymentProcessed(id, status, updatedAt);
  }
}
