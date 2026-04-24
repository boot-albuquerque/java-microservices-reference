package com.example.payment.domain.model;

import com.example.payment.domain.exception.InvalidStateTransitionException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class PaymentStateMachine {

  private static final Map<PaymentStatus, Set<PaymentStatus>> TRANSITIONS;

  static {
    Map<PaymentStatus, Set<PaymentStatus>> map = new EnumMap<>(PaymentStatus.class);
    map.put(PaymentStatus.PENDING, EnumSet.of(PaymentStatus.PROCESSING));
    map.put(PaymentStatus.PROCESSING, EnumSet.of(PaymentStatus.COMPLETED, PaymentStatus.FAILED));
    map.put(PaymentStatus.COMPLETED, EnumSet.noneOf(PaymentStatus.class));
    map.put(PaymentStatus.FAILED, EnumSet.noneOf(PaymentStatus.class));
    TRANSITIONS = Map.copyOf(map);
  }

  private PaymentStateMachine() {}

  public static void assertValidTransition(PaymentStatus from, PaymentStatus to) {
    Set<PaymentStatus> allowed =
        TRANSITIONS.getOrDefault(from, EnumSet.noneOf(PaymentStatus.class));
    if (!allowed.contains(to)) {
      throw new InvalidStateTransitionException(from, to);
    }
  }
}
