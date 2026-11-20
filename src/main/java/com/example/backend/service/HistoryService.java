package com.example.backend.service;

import com.example.backend.dto.HistoryMessage;
import com.example.backend.dto.HistoryResponse;
import com.example.backend.exception.BusinessException;
import com.example.backend.repository.SessionRepository;
import com.example.backend.store.memory.chat.PersistentChatMemoryStore;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryService {

    private final SessionRepository sessionRepository;
    private final PersistentChatMemoryStore chatMemoryStore;

    public HistoryResponse getHistory(String sessionId) {
        log.info("[History] get sessionId={}", sessionId);
        requireSession(sessionId);
        List<HistoryMessage> messages = chatMemoryStore.getMessages(sessionId).stream()
                .map(msg -> new HistoryMessage(mapRole(msg.type()), extractText(msg)))
                .toList();
        return new HistoryResponse(sessionId, messages.size(), messages);
    }

    public String exportAsText(String sessionId) {
        log.info("[Export] sessionId={}", sessionId);
        requireSession(sessionId);
        return chatMemoryStore.getMessages(sessionId).stream()
                .filter(m -> m.type() != ChatMessageType.SYSTEM)
                .map(m -> mapRole(m.type()) + ": " + extractText(m))
                .collect(Collectors.joining("\n\n"));
    }

    private void requireSession(String sessionId) {
        if (sessionRepository.findById(sessionId).isEmpty()) {
            throw BusinessException.notFound("会话不存在");
        }
    }

    private String mapRole(ChatMessageType type) {
        return switch (type) {
            case USER -> "User";
            case AI -> "AI";
            case SYSTEM -> "System";
            case TOOL_EXECUTION_RESULT -> "Tool";
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    private String extractText(ChatMessage message) {
        return switch (message) {
            case UserMessage m -> m.singleText();
            case AiMessage m -> m.text();
            case SystemMessage m -> m.text();
            case ToolExecutionResultMessage m -> m.text();
            default -> "";
        };
    }
}
