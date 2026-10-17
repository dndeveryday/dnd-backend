package com.example.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(UUID id, String name, int quantity, BigDecimal unitPrice) {
}
