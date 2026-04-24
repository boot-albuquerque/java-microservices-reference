package com.example.payment.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.payment.domain.exception.InvalidStateTransitionException;
import com.example.payment.domain.model.PaymentStateMachine;
import com.example.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;

class PaymentStateMachineTest {

  @Test
  void shouldAllowPendingToProcessing() {
    assertThatCode(
            () ->
                PaymentStateMachine.assertValidTransition(
                    PaymentStatus.PENDING, PaymentStatus.PROCESSING))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAllowProcessingToCompleted() {
    assertThatCode(
            () ->
                PaymentStateMachine.assertValidTransition(
                    PaymentStatus.PROCESSING, PaymentStatus.COMPLETED))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAllowProcessingToFailed() {
    assertThatCode(
            () ->
                PaymentStateMachine.assertValidTransition(
                    PaymentStatus.PROCESSING, PaymentStatus.FAILED))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectPendingToCompleted() {
    assertThatThrownBy(
            () ->
                PaymentStateMachine.assertValidTransition(
                    PaymentStatus.PENDING, PaymentStatus.COMPLETED))
        .isInstanceOf(InvalidStateTransitionException.class)
        .hasMessageContaining("PENDING")
        .hasMessageContaining("COMPLETED");
  }

  @Test
  void shouldRejectCompletedToAnything() {
    assertThatThrownBy(
            () ->
                PaymentStateMachine.assertValidTransition(
                    PaymentStatus.COMPLETED, PaymentStatus.PENDING))
        .isInstanceOf(InvalidStateTransitionException.class);
    assertThatThrownBy(
            () ->
                PaymentStateMachine.assertValidTransition(
                    PaymentStatus.COMPLETED, PaymentStatus.PROCESSING))
        .isInstanceOf(InvalidStateTransitionException.class);
    assertThatThrownBy(
            () ->
                PaymentStateMachine.assertValidTransition(
                    PaymentStatus.COMPLETED, PaymentStatus.FAILED))
        .isInstanceOf(InvalidStateTransitionException.class);
  }

  @Test
  void shouldRejectFailedToAnything() {
    assertThatThrownBy(
            () ->
                PaymentStateMachine.assertValidTransition(
                    PaymentStatus.FAILED, PaymentStatus.PENDING))
        .isInstanceOf(InvalidStateTransitionException.class);
    assertThatThrownBy(
            () ->
                PaymentStateMachine.assertValidTransition(
                    PaymentStatus.FAILED, PaymentStatus.PROCESSING))
        .isInstanceOf(InvalidStateTransitionException.class);
    assertThatThrownBy(
            () ->
                PaymentStateMachine.assertValidTransition(
                    PaymentStatus.FAILED, PaymentStatus.COMPLETED))
        .isInstanceOf(InvalidStateTransitionException.class);
  }

  @Test
  void shouldRejectSameStateTransition() {
    assertThatThrownBy(
            () ->
                PaymentStateMachine.assertValidTransition(
                    PaymentStatus.PENDING, PaymentStatus.PENDING))
        .isInstanceOf(InvalidStateTransitionException.class);
  }
}
