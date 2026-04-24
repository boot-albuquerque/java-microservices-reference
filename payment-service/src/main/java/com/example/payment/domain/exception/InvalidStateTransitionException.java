package com.example.payment.domain.exception;

import com.example.payment.domain.model.PaymentStatus;

public class InvalidStateTransitionException extends DomainException {

  public InvalidStateTransitionException(PaymentStatus from, PaymentStatus to) {
    super("Invalid state transition: " + from + " -> " + to);
  }
}
