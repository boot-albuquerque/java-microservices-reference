package com.example.payment.infrastructure.persistence;

import com.example.payment.domain.model.Payment;
import com.example.payment.domain.port.PaymentRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepositoryAdapter implements PaymentRepository {

  private final PaymentJpaRepository jpa;

  public PaymentRepositoryAdapter(PaymentJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Payment> findById(UUID id) {
    return jpa.findById(id).map(PaymentEntity::toDomain);
  }

  @Override
  public Optional<Payment> findByIdempotencyKey(UUID key) {
    return jpa.findByIdempotencyKey(key).map(PaymentEntity::toDomain);
  }

  @Override
  public Payment save(Payment payment) {
    return jpa.save(PaymentEntity.from(payment)).toDomain();
  }
}
