package com.example.payment.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCreated(
    UUID paymentId,
    BigDecimal amount,
    String currency,
    UUID payerId,
    UUID payeeId,
    String externalReference,
    Instant timestamp) {}
