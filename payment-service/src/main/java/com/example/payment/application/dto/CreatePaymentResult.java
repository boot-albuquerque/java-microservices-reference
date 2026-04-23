package com.example.payment.application.dto;

public record CreatePaymentResult(PaymentResponse response, boolean wasIdempotent) {}
