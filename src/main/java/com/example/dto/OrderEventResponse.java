package com.example.dto;

import com.example.entity.OrderStatus;
import java.time.Instant;
import java.util.UUID;

public record OrderEventResponse(UUID id, OrderStatus status, String description, Instant occurredAt) {
}
