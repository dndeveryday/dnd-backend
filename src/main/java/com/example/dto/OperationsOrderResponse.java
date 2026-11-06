package com.example.dto;

import com.example.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OperationsOrderResponse(
        UUID id,
        String orderNumber,
        String serviceType,
        OrderStatus status,
        UUID customerId,
        String customerName,
        String location,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt,
        Instant updatedAt) {
}