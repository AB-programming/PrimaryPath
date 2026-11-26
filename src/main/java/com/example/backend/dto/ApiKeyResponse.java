package com.example.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 凭证元数据，不含哈希 */
public record ApiKeyResponse(
        String id,
        String name,
        List<String> scopes,
        String status,
        LocalDateTime expiresAt,
        LocalDateTime lastUsedAt,
        LocalDateTime createdAt,
        LocalDateTime revokedAt) {
}
