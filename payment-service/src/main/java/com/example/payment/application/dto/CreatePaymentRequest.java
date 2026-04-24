package com.example.payment.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
    @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal amount,
    @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
    @NotNull UUID payerId,
    @NotNull UUID payeeId,
    @Size(max = 255) String externalReference) {}
