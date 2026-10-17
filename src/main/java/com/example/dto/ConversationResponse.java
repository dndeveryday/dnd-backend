package com.example.dto;

import com.example.entity.ConversationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        ConversationStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<MessageResponse> messages) {
}
