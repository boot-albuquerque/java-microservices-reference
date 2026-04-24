package com.example.payment.domain.port;

import com.example.payment.domain.model.Payment;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

  Optional<Payment> findById(UUID id);

  Optional<Payment> findByIdempotencyKey(UUID key);

  Payment save(Payment payment);
}
