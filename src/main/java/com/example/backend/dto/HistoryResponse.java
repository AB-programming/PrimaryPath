package com.example.backend.dto;

import java.util.List;

/**
 * 会话历史响应。
 */
public record HistoryResponse(String sessionId, int messageCount, List<HistoryMessage> messages) {
}
