package com.example.dto;

import com.example.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String providerReference,
        Instant createdAt,
        Instant paidAt) {
}
