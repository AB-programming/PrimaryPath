package com.example.backend.dto;

/** apiKey 是完整 key 明文，只在发放响应里出现这一次 */
public record IssuedApiKeyResponse(ApiKeyResponse key, String apiKey) {
}
