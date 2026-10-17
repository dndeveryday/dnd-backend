package com.example.dto;

import com.example.entity.TicketStatus;
import java.time.Instant;
import java.util.UUID;

public record TicketEventResponse(
        UUID id,
        String eventType,
        TicketStatus status,
        String detail,
        Instant occurredAt) {
}
