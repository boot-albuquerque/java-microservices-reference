package com.example.payment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.payment.AbstractIntegrationTest;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.model.PaymentStatus;
import com.example.payment.domain.port.PaymentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PaymentRepositoryAdapterIT extends AbstractIntegrationTest {

  @Autowired private PaymentRepository repository;

  @Test
  void shouldSaveAndFindById() {
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
            Instant.now());

    repository.save(payment);

    Optional<Payment> found = repository.findById(id);
    assertThat(found).isPresent();
    assertThat(found.get().id()).isEqualTo(id);
    assertThat(found.get().status()).isEqualTo(PaymentStatus.PENDING);
  }

  @Test
  void shouldFindByIdempotencyKey() {
    UUID idempotencyKey = UUID.randomUUID();
    Payment payment =
        Payment.createPending(
            UUID.randomUUID(),
            new BigDecimal("50.00"),
            "USD",
            idempotencyKey,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ref-it",
            Instant.now());

    repository.save(payment);

    Optional<Payment> found = repository.findByIdempotencyKey(idempotencyKey);
    assertThat(found).isPresent();
    assertThat(found.get().idempotencyKey()).isEqualTo(idempotencyKey);
  }
}
