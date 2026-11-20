package com.example.backend.service;

import com.example.backend.ai.Assistant;

import dev.langchain4j.service.TokenStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final Assistant assistant;
    private final SessionService sessionService;

    public String chat(String sessionId, String message) {
        log.info("[Chat] sessionId={}, message={}", sessionId, message);
        sessionService.ensureSession(sessionId, message);
        String response = assistant.chat(sessionId, message);
        sessionService.touchUpdatedAt(sessionId);
        log.info("[Chat] sessionId={}, response={}", sessionId, response);
        return response;
    }

    /**
     * 发起流式对话，返回 {@link TokenStream}。
     * 会话保障（ensureSession）在 Service 层完成，
     * TokenStream 与 SSE 传输的绑定由 Controller 层负责。
     */
    public TokenStream chatStream(String sessionId, String message) {
        log.info("[Stream] sessionId={}, message={}", sessionId, message);
        sessionService.ensureSession(sessionId, message);
        return assistant.chatStream(sessionId, message);
    }

    public void markSessionActive(String sessionId) {
        sessionService.touchUpdatedAt(sessionId);
    }
}
