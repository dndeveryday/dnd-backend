package com.example.dto;

import com.example.entity.MessageSender;
import java.time.Instant;
import java.util.UUID;

public record MessageResponse(UUID id, MessageSender sender, String content, Instant createdAt) {
}
