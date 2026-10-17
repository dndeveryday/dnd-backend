package com.example.dto;

import com.example.entity.TicketStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        UUID orderId,
        UUID conversationId,
        String category,
        String summary,
        String description,
        TicketStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<TicketEventResponse> events) {
}
