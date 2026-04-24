package com.example.payment.application.usecase;

import com.example.payment.application.dto.CreatePaymentRequest;
import com.example.payment.application.dto.CreatePaymentResult;
import com.example.payment.application.dto.PaymentResponse;
import com.example.payment.application.exception.IdempotencyConflictException;
import com.example.payment.application.port.MetricsRecorder;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.port.EventPublisher;
import com.example.payment.domain.port.IdempotencyStore;
import com.example.payment.domain.port.PaymentRepository;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

public class CreatePaymentUseCase {

  private final PaymentRepository repository;
  private final EventPublisher eventPublisher;
  private final IdempotencyStore idempotencyStore;
  private final Clock clock;
  private final MetricsRecorder metrics;

  public CreatePaymentUseCase(
      PaymentRepository repository,
      EventPublisher eventPublisher,
      IdempotencyStore idempotencyStore,
      Clock clock,
      MetricsRecorder metrics) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
    this.idempotencyStore = idempotencyStore;
    this.clock = clock;
    this.metrics = metrics;
  }

  public CreatePaymentResult execute(
      UUID userId, UUID idempotencyKey, CreatePaymentRequest request) {
    Optional<UUID> existingPaymentId = idempotencyStore.find(idempotencyKey);
    if (existingPaymentId.isPresent()) {
      PaymentResponse existing =
          repository
              .findById(existingPaymentId.get())
              .map(PaymentResponse::from)
              .orElseThrow(() -> new IdempotencyConflictException(idempotencyKey));
      return new CreatePaymentResult(existing, true);
    }

    UUID paymentId = UUID.randomUUID();

    // Atomically claim the idempotency key before touching DB — prevents duplicate rows
    boolean stored = idempotencyStore.storeIfAbsent(idempotencyKey, paymentId);
    if (!stored) {
      UUID winnerId =
          idempotencyStore
              .find(idempotencyKey)
              .orElseThrow(() -> new IdempotencyConflictException(idempotencyKey));
      // Winner thread may not have persisted yet; retry briefly
      PaymentResponse winner = null;
      for (int attempt = 0; attempt < 10 && winner == null; attempt++) {
        winner = repository.findById(winnerId).map(PaymentResponse::from).orElse(null);
        if (winner == null) {
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }
      if (winner == null) throw new IdempotencyConflictException(idempotencyKey);
      return new CreatePaymentResult(winner, true);
    }

    Payment payment =
        Payment.createPending(
            paymentId,
            request.amount(),
            request.currency(),
            idempotencyKey,
            request.payerId(),
            request.payeeId(),
            request.externalReference(),
            clock.instant());

    repository.save(payment);
    eventPublisher.publishCreated(payment.toCreatedEvent());
    metrics.recordPaymentCreated();

    return new CreatePaymentResult(PaymentResponse.from(payment), false);
  }
}
