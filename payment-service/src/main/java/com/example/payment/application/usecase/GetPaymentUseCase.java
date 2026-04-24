package com.example.payment.application.usecase;

import com.example.payment.application.dto.PaymentResponse;
import com.example.payment.domain.exception.PaymentNotFoundException;
import com.example.payment.domain.port.PaymentRepository;
import java.util.UUID;

public class GetPaymentUseCase {

  private final PaymentRepository repository;

  public GetPaymentUseCase(PaymentRepository repository) {
    this.repository = repository;
  }

  public PaymentResponse execute(UUID id) {
    return repository
        .findById(id)
        .map(PaymentResponse::from)
        .orElseThrow(() -> new PaymentNotFoundException(id));
  }
}
