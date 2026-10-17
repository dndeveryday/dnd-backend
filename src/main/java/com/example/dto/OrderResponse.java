package com.example.dto;

import com.example.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        String serviceType,
        OrderStatus status,
        String location,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt,
        List<OrderItemResponse> items,
        List<OrderEventResponse> events,
        List<PaymentResponse> payments) {
}
