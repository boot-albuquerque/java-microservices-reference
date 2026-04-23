package com.example.payment.presentation.controller;

import com.example.payment.application.dto.CreatePaymentRequest;
import com.example.payment.application.dto.CreatePaymentResult;
import com.example.payment.application.dto.PaymentResponse;
import com.example.payment.application.usecase.CreatePaymentUseCase;
import com.example.payment.application.usecase.GetPaymentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

  private final CreatePaymentUseCase createPaymentUseCase;
  private final GetPaymentUseCase getPaymentUseCase;

  public PaymentController(
      CreatePaymentUseCase createPaymentUseCase, GetPaymentUseCase getPaymentUseCase) {
    this.createPaymentUseCase = createPaymentUseCase;
    this.getPaymentUseCase = getPaymentUseCase;
  }

  @PostMapping
  @Operation(summary = "Create a payment")
  public ResponseEntity<PaymentResponse> createPayment(
      @RequestHeader("Idempotency-Key") String idempotencyKeyHeader,
      @Valid @RequestBody CreatePaymentRequest request,
      Authentication authentication) {

    UUID idempotencyKey;
    try {
      idempotencyKey = UUID.fromString(idempotencyKeyHeader);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Idempotency-Key must be a valid UUID");
    }

    UUID userId = (UUID) authentication.getPrincipal();
    CreatePaymentResult result = createPaymentUseCase.execute(userId, idempotencyKey, request);

    if (result.wasIdempotent()) {
      return ResponseEntity.ok(result.response());
    }
    return ResponseEntity.status(201).body(result.response());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a payment by ID")
  public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID id) {
    return ResponseEntity.ok(getPaymentUseCase.execute(id));
  }
}
