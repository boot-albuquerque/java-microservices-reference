package com.example.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.payment.application.dto.PaymentResponse;
import com.example.payment.application.port.MetricsRecorder;
import com.example.payment.application.usecase.ProcessPaymentUseCase;
import com.example.payment.domain.event.PaymentProcessed;
import com.example.payment.domain.exception.PaymentNotFoundException;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.model.PaymentStatus;
import com.example.payment.domain.port.EventPublisher;
import com.example.payment.domain.port.PaymentRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentUseCaseTest {

  @Mock private PaymentRepository repository;
  @Mock private EventPublisher eventPublisher;
  @Mock private MetricsRecorder metrics;

  private Clock clock;
  private ProcessPaymentUseCase useCase;

  private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(NOW, ZoneOffset.UTC);
    useCase = new ProcessPaymentUseCase(repository, eventPublisher, clock, metrics);
  }

  private Payment pendingPayment(UUID id) {
    return Payment.createPending(
        id,
        new BigDecimal("100.00"),
        "BRL",
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        null,
        NOW);
  }

  @Test
  void shouldPublishPaymentProcessedOnCompleted() {
    UUID paymentId = UUID.randomUUID();
    when(repository.findById(paymentId)).thenReturn(Optional.of(pendingPayment(paymentId)));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    useCase.execute(paymentId, false);

    ArgumentCaptor<PaymentProcessed> captor = ArgumentCaptor.forClass(PaymentProcessed.class);
    verify(eventPublisher).publishProcessed(captor.capture());
    assertThat(captor.getValue().status()).isEqualTo(PaymentStatus.COMPLETED);
    verify(metrics).recordPaymentProcessed(PaymentStatus.COMPLETED.name());
  }

  @Test
  void shouldPublishPaymentProcessedOnFailed() {
    UUID paymentId = UUID.randomUUID();
    when(repository.findById(paymentId)).thenReturn(Optional.of(pendingPayment(paymentId)));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    useCase.execute(paymentId, true);

    ArgumentCaptor<PaymentProcessed> captor = ArgumentCaptor.forClass(PaymentProcessed.class);
    verify(eventPublisher).publishProcessed(captor.capture());
    assertThat(captor.getValue().status()).isEqualTo(PaymentStatus.FAILED);
    verify(metrics).recordPaymentProcessed(PaymentStatus.FAILED.name());
  }

  @Test
  void shouldTransitionThroughProcessing() {
    UUID paymentId = UUID.randomUUID();
    when(repository.findById(paymentId)).thenReturn(Optional.of(pendingPayment(paymentId)));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    PaymentResponse response = useCase.execute(paymentId, false);

    verify(repository, times(2)).save(any(Payment.class));
    assertThat(response.status()).isEqualTo(PaymentStatus.COMPLETED.name());
  }

  @Test
  void shouldThrowWhenPaymentNotFound() {
    UUID paymentId = UUID.randomUUID();
    when(repository.findById(paymentId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(paymentId, false))
        .isInstanceOf(PaymentNotFoundException.class);
  }
}
