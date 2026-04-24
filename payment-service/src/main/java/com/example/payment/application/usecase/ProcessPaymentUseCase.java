package com.example.payment.application.usecase;

import com.example.payment.application.dto.PaymentResponse;
import com.example.payment.application.port.MetricsRecorder;
import com.example.payment.domain.exception.PaymentNotFoundException;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.model.PaymentStatus;
import com.example.payment.domain.port.EventPublisher;
import com.example.payment.domain.port.PaymentRepository;
import java.time.Clock;
import java.util.UUID;

public class ProcessPaymentUseCase {

  private final PaymentRepository repository;
  private final EventPublisher eventPublisher;
  private final Clock clock;
  private final MetricsRecorder metrics;

  public ProcessPaymentUseCase(
      PaymentRepository repository,
      EventPublisher eventPublisher,
      Clock clock,
      MetricsRecorder metrics) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
    this.metrics = metrics;
  }

  public PaymentResponse execute(UUID paymentId, boolean shouldFail) {
    Payment payment =
        repository.findById(paymentId).orElseThrow(() -> new PaymentNotFoundException(paymentId));

    Payment processing = payment.withStatus(PaymentStatus.PROCESSING, clock.instant());
    repository.save(processing);

    PaymentStatus finalStatus = shouldFail ? PaymentStatus.FAILED : PaymentStatus.COMPLETED;
    Payment finalPayment = processing.withStatus(finalStatus, clock.instant());
    repository.save(finalPayment);

    eventPublisher.publishProcessed(finalPayment.toProcessedEvent());
    metrics.recordPaymentProcessed(finalStatus.name());

    return PaymentResponse.from(finalPayment);
  }
}
