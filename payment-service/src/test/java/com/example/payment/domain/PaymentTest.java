package com.example.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.payment.domain.event.PaymentCreated;
import com.example.payment.domain.event.PaymentProcessed;
import com.example.payment.domain.exception.InvalidStateTransitionException;
import com.example.payment.domain.exception.PaymentNotFoundException;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.model.PaymentStatus;
import com.example.payment.domain.port.RateLimiter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentTest {

  private static final UUID ID = UUID.randomUUID();
  private static final UUID IDEMPOTENCY_KEY = UUID.randomUUID();
  private static final UUID PAYER_ID = UUID.randomUUID();
  private static final UUID PAYEE_ID = UUID.randomUUID();
  private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

  @Test
  void shouldCreatePendingPayment() {
    Payment payment =
        Payment.createPending(
            ID, new BigDecimal("100.00"), "BRL", IDEMPOTENCY_KEY, PAYER_ID, PAYEE_ID, null, NOW);

    assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING);
    assertThat(payment.createdAt()).isEqualTo(NOW);
    assertThat(payment.updatedAt()).isEqualTo(NOW);
    assertThat(payment.id()).isEqualTo(ID);
  }

  @Test
  void shouldRejectNegativeAmount() {
    assertThatThrownBy(
            () ->
                Payment.createPending(
                    ID,
                    new BigDecimal("-1.00"),
                    "BRL",
                    IDEMPOTENCY_KEY,
                    PAYER_ID,
                    PAYEE_ID,
                    null,
                    NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectZeroAmount() {
    assertThatThrownBy(
            () ->
                Payment.createPending(
                    ID, BigDecimal.ZERO, "BRL", IDEMPOTENCY_KEY, PAYER_ID, PAYEE_ID, null, NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectInvalidCurrency() {
    assertThatThrownBy(
            () ->
                Payment.createPending(
                    ID, new BigDecimal("10"), "BR", IDEMPOTENCY_KEY, PAYER_ID, PAYEE_ID, null, NOW))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                Payment.createPending(
                    ID,
                    new BigDecimal("10"),
                    "brl",
                    IDEMPOTENCY_KEY,
                    PAYER_ID,
                    PAYEE_ID,
                    null,
                    NOW))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                Payment.createPending(
                    ID, new BigDecimal("10"), null, IDEMPOTENCY_KEY, PAYER_ID, PAYEE_ID, null, NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullUuids() {
    assertThatThrownBy(
            () ->
                Payment.createPending(
                    null,
                    new BigDecimal("10"),
                    "BRL",
                    IDEMPOTENCY_KEY,
                    PAYER_ID,
                    PAYEE_ID,
                    null,
                    NOW))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                Payment.createPending(
                    ID, new BigDecimal("10"), "BRL", null, PAYER_ID, PAYEE_ID, null, NOW))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                Payment.createPending(
                    ID, new BigDecimal("10"), "BRL", IDEMPOTENCY_KEY, null, PAYEE_ID, null, NOW))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                Payment.createPending(
                    ID, new BigDecimal("10"), "BRL", IDEMPOTENCY_KEY, PAYER_ID, null, null, NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldAllowNullExternalReference() {
    Payment payment =
        Payment.createPending(
            ID, new BigDecimal("10"), "BRL", IDEMPOTENCY_KEY, PAYER_ID, PAYEE_ID, null, NOW);
    assertThat(payment.externalReference()).isNull();
  }

  @Test
  void shouldTransitionToProcessing() {
    Payment pending =
        Payment.createPending(
            ID, new BigDecimal("100.00"), "BRL", IDEMPOTENCY_KEY, PAYER_ID, PAYEE_ID, null, NOW);
    Instant later = NOW.plusSeconds(60);

    Payment processing = pending.withStatus(PaymentStatus.PROCESSING, later);

    assertThat(processing.status()).isEqualTo(PaymentStatus.PROCESSING);
    assertThat(processing.updatedAt()).isEqualTo(later);
    assertThat(processing.createdAt()).isEqualTo(NOW);
  }

  @Test
  void shouldRejectInvalidTransition() {
    Payment pending =
        Payment.createPending(
            ID, new BigDecimal("100.00"), "BRL", IDEMPOTENCY_KEY, PAYER_ID, PAYEE_ID, null, NOW);

    assertThatThrownBy(() -> pending.withStatus(PaymentStatus.COMPLETED, NOW))
        .isInstanceOf(InvalidStateTransitionException.class);
  }

  @Test
  void shouldCreatePaymentNotFoundException() {
    UUID missingId = UUID.randomUUID();
    PaymentNotFoundException ex = new PaymentNotFoundException(missingId);
    assertThat(ex.getMessage()).contains(missingId.toString());
    assertThat(ex).isInstanceOf(com.example.payment.domain.exception.DomainException.class);
  }

  @Test
  void shouldCreateDomainEvents() {
    PaymentCreated created =
        new PaymentCreated(ID, new BigDecimal("10"), "BRL", PAYER_ID, PAYEE_ID, null, NOW);
    assertThat(created.paymentId()).isEqualTo(ID);

    PaymentProcessed processed = new PaymentProcessed(ID, PaymentStatus.COMPLETED, NOW);
    assertThat(processed.status()).isEqualTo(PaymentStatus.COMPLETED);
  }

  @Test
  void shouldCreateRateLimitResult() {
    RateLimiter.RateLimitResult result = new RateLimiter.RateLimitResult(true, 99L, 100L);
    assertThat(result.allowed()).isTrue();
    assertThat(result.remaining()).isEqualTo(99L);
    assertThat(result.limit()).isEqualTo(100L);
  }

  @Test
  void toCreatedEventShouldContainAllFields() {
    Payment payment =
        Payment.createPending(
            ID, new BigDecimal("50.00"), "USD", IDEMPOTENCY_KEY, PAYER_ID, PAYEE_ID, "ref-1", NOW);

    PaymentCreated event = payment.toCreatedEvent();

    assertThat(event.paymentId()).isEqualTo(ID);
    assertThat(event.amount()).isEqualByComparingTo(new BigDecimal("50.00"));
    assertThat(event.currency()).isEqualTo("USD");
    assertThat(event.payerId()).isEqualTo(PAYER_ID);
    assertThat(event.payeeId()).isEqualTo(PAYEE_ID);
    assertThat(event.externalReference()).isEqualTo("ref-1");
    assertThat(event.timestamp()).isEqualTo(NOW);
  }

  @Test
  void toProcessedEventShouldReflectFinalStatus() {
    Payment pending =
        Payment.createPending(
            ID, new BigDecimal("50.00"), "USD", IDEMPOTENCY_KEY, PAYER_ID, PAYEE_ID, null, NOW);
    Instant later = NOW.plusSeconds(10);
    Payment processing = pending.withStatus(PaymentStatus.PROCESSING, later);
    Instant done = later.plusSeconds(5);
    Payment completed = processing.withStatus(PaymentStatus.COMPLETED, done);

    PaymentProcessed event = completed.toProcessedEvent();

    assertThat(event.paymentId()).isEqualTo(ID);
    assertThat(event.status()).isEqualTo(PaymentStatus.COMPLETED);
    assertThat(event.timestamp()).isEqualTo(done);
  }
}
