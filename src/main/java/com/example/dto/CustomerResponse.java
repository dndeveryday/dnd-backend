package com.example.dto;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(UUID id, String email, String displayName, Instant createdAt) {
}
