package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record TicketRequest(
        @NotBlank @Size(max = 80) String category,
        @NotBlank @Size(max = 240) String summary,
        @NotBlank @Size(max = 5000) String description,
        UUID orderId,
        UUID conversationId) {
}
