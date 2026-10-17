package com.example.dto;

import com.example.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        String orderNumber,
        String serviceType,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt) {
}
