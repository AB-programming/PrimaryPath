package com.example.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/** scopes 取值见 ApiScope；expiresAt 可空 */
public record CreateApiKeyRequest(String name, List<String> scopes, LocalDateTime expiresAt) {
}
