package com.example.payment.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

  Optional<PaymentEntity> findByIdempotencyKey(UUID idempotencyKey);
}
