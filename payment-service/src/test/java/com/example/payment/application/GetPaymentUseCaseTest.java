package com.example.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.payment.application.dto.PaymentResponse;
import com.example.payment.application.usecase.GetPaymentUseCase;
import com.example.payment.domain.exception.PaymentNotFoundException;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.port.PaymentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetPaymentUseCaseTest {

  @Mock private PaymentRepository repository;

  private GetPaymentUseCase useCase;

  private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

  @BeforeEach
  void setUp() {
    useCase = new GetPaymentUseCase(repository);
  }

  @Test
  void shouldReturnPaymentWhenFound() {
    UUID id = UUID.randomUUID();
    Payment payment =
        Payment.createPending(
            id,
            new BigDecimal("100.00"),
            "BRL",
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            NOW);
    when(repository.findById(id)).thenReturn(Optional.of(payment));

    PaymentResponse response = useCase.execute(id);

    assertThat(response.id()).isEqualTo(id);
    assertThat(response.status()).isEqualTo("PENDING");
  }

  @Test
  void shouldThrowWhenNotFound() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(id)).isInstanceOf(PaymentNotFoundException.class);
  }
}
