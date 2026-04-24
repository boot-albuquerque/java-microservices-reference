package com.example.payment.domain.port;

import com.example.payment.domain.event.PaymentCreated;
import com.example.payment.domain.event.PaymentProcessed;

public interface EventPublisher {

  /**
   * Publishes a PaymentCreated event. Implementations must NOT propagate exceptions — failures must
   * be absorbed (logged or stored) so the calling use case completes successfully.
   */
  void publishCreated(PaymentCreated event);

  /**
   * Publishes a PaymentProcessed event. Implementations must NOT propagate exceptions — failures
   * must be absorbed (logged or stored) so the calling use case completes successfully.
   */
  void publishProcessed(PaymentProcessed event);
}
