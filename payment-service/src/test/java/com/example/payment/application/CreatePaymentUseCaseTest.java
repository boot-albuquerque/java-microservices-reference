package com.example.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.payment.application.dto.CreatePaymentRequest;
import com.example.payment.application.dto.CreatePaymentResult;
import com.example.payment.application.exception.IdempotencyConflictException;
import com.example.payment.application.usecase.CreatePaymentUseCase;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.model.PaymentStatus;
import com.example.payment.domain.port.EventPublisher;
import com.example.payment.domain.port.IdempotencyStore;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreatePaymentUseCaseTest {

  @Mock private PaymentRepository repository;
  @Mock private EventPublisher eventPublisher;
  @Mock private IdempotencyStore idempotencyStore;

  private Clock clock;
  private CreatePaymentUseCase useCase;

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID IDEMPOTENCY_KEY = UUID.randomUUID();
  private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(NOW, ZoneOffset.UTC);
    useCase = new CreatePaymentUseCase(repository, eventPublisher, idempotencyStore, clock);
  }

  @Test
  void shouldCreatePaymentWhenNotIdempotent() {
    when(idempotencyStore.find(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    CreatePaymentRequest request =
        new CreatePaymentRequest(
            new BigDecimal("100.00"), "BRL", UUID.randomUUID(), UUID.randomUUID(), null);

    CreatePaymentResult result = useCase.execute(USER_ID, IDEMPOTENCY_KEY, request);

    assertThat(result.wasIdempotent()).isFalse();
    assertThat(result.response().status()).isEqualTo("PENDING");
    verify(repository).save(any(Payment.class));
    verify(idempotencyStore).store(eq(IDEMPOTENCY_KEY), any(UUID.class));
    verify(eventPublisher).publishCreated(any());
  }

  @Test
  void shouldReturnExistingWithIdempotentFlagWhenKeyExists() {
    UUID existingPaymentId = UUID.randomUUID();
    Payment existing =
        Payment.createPending(
            existingPaymentId,
            new BigDecimal("50.00"),
            "USD",
            IDEMPOTENCY_KEY,
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            NOW);

    when(idempotencyStore.find(IDEMPOTENCY_KEY)).thenReturn(Optional.of(existingPaymentId));
    when(repository.findById(existingPaymentId)).thenReturn(Optional.of(existing));

    CreatePaymentRequest request =
        new CreatePaymentRequest(
            new BigDecimal("50.00"), "USD", UUID.randomUUID(), UUID.randomUUID(), null);

    CreatePaymentResult result = useCase.execute(USER_ID, IDEMPOTENCY_KEY, request);

    assertThat(result.wasIdempotent()).isTrue();
    assertThat(result.response().id()).isEqualTo(existingPaymentId);
    verify(repository, never()).save(any());
    verify(eventPublisher, never()).publishCreated(any());
  }

  @Test
  void shouldThrowIdempotencyConflictWhenKeyExistsButPaymentMissing() {
    UUID existingPaymentId = UUID.randomUUID();
    when(idempotencyStore.find(IDEMPOTENCY_KEY)).thenReturn(Optional.of(existingPaymentId));
    when(repository.findById(existingPaymentId)).thenReturn(Optional.empty());

    CreatePaymentRequest request =
        new CreatePaymentRequest(
            new BigDecimal("10.00"), "BRL", UUID.randomUUID(), UUID.randomUUID(), null);

    assertThatThrownBy(() -> useCase.execute(USER_ID, IDEMPOTENCY_KEY, request))
        .isInstanceOf(IdempotencyConflictException.class);
  }

  @Test
  void shouldProduceCorrectPaymentResponse() {
    UUID payerId = UUID.randomUUID();
    UUID payeeId = UUID.randomUUID();
    when(idempotencyStore.find(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    CreatePaymentRequest request =
        new CreatePaymentRequest(new BigDecimal("200.00"), "EUR", payerId, payeeId, "ref-123");

    CreatePaymentResult result = useCase.execute(USER_ID, IDEMPOTENCY_KEY, request);

    assertThat(result.wasIdempotent()).isFalse();
    assertThat(result.response().amount()).isEqualByComparingTo(new BigDecimal("200.00"));
    assertThat(result.response().currency()).isEqualTo("EUR");
    assertThat(result.response().payerId()).isEqualTo(payerId);
    assertThat(result.response().payeeId()).isEqualTo(payeeId);
    assertThat(result.response().externalReference()).isEqualTo("ref-123");
    assertThat(result.response().status()).isEqualTo(PaymentStatus.PENDING.name());
    assertThat(result.response().createdAt()).isEqualTo(NOW);
  }
}
