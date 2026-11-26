package com.example.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TenantResponse(
        String id,
        String name,
        String status,
        LocalDateTime createdAt,
        List<ApiKeyResponse> keys) {
}
