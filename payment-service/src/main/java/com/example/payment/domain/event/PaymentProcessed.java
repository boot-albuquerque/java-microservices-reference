package com.example.payment.domain.event;

import com.example.payment.domain.model.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

public record PaymentProcessed(UUID paymentId, PaymentStatus status, Instant timestamp) {}
