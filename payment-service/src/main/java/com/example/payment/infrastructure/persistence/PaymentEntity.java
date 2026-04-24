package com.example.payment.infrastructure.persistence;

import com.example.payment.domain.model.Payment;
import com.example.payment.domain.model.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentEntity {

  @Id private UUID id;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 3)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PaymentStatus status;

  @Column(name = "idempotency_key", nullable = false, unique = true)
  private UUID idempotencyKey;

  @Column(name = "payer_id", nullable = false)
  private UUID payerId;

  @Column(name = "payee_id", nullable = false)
  private UUID payeeId;

  @Column(name = "external_reference")
  private String externalReference;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected PaymentEntity() {}

  public static PaymentEntity from(Payment payment) {
    PaymentEntity entity = new PaymentEntity();
    entity.id = payment.id();
    entity.amount = payment.amount();
    entity.currency = payment.currency();
    entity.status = payment.status();
    entity.idempotencyKey = payment.idempotencyKey();
    entity.payerId = payment.payerId();
    entity.payeeId = payment.payeeId();
    entity.externalReference = payment.externalReference();
    entity.createdAt = payment.createdAt();
    entity.updatedAt = payment.updatedAt();
    return entity;
  }

  public Payment toDomain() {
    return Payment.reconstitute(
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

  public UUID getId() {
    return id;
  }
}
