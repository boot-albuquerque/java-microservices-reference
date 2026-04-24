package com.example.payment.presentation.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.payment.domain.exception.InvalidStateTransitionException;
import com.example.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void shouldReturn409OnInvalidStateTransition() {
    InvalidStateTransitionException ex =
        new InvalidStateTransitionException(PaymentStatus.PENDING, PaymentStatus.COMPLETED);

    ResponseEntity<ErrorResponse> response = handler.handleInvalidStateTransition(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(409);
    assertThat(response.getBody().error()).isEqualTo("Conflict");
    assertThat(response.getBody().message()).contains("PENDING");
  }
}
