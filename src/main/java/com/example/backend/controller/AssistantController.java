package com.example.backend.controller;

import com.example.backend.service.ChatService;

import dev.langchain4j.service.TokenStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final ChatService chatService;

    @GetMapping("/chat")
    public String chat(@RequestParam String message,
                       @RequestParam(defaultValue = "default") String sessionId) {
        return chatService.chat(sessionId, message);
    }

    @GetMapping("/chat/stream")
    public SseEmitter chatStream(@RequestParam String message,
                                 @RequestParam(defaultValue = "default") String sessionId) {
        SseEmitter emitter = new SseEmitter(0L);
        TokenStream tokenStream = chatService.chatStream(sessionId, message);
        tokenStream
                .onPartialResponse(token -> {
                    try {
                        emitter.send(SseEmitter.event().data(token));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                })
                .onToolExecuted(toolExecution -> {
                    log.info("[Tool] {} args={} → result={}",
                            toolExecution.request().name(),
                            toolExecution.request().arguments(),
                            toolExecution.result());
                })
                .onCompleteResponse(response -> {
                    log.info("[Stream] sessionId={}, completed", sessionId);
                    chatService.markSessionActive(sessionId);
                    emitter.complete();
                })
                .onError(error -> {
                    log.error("[Stream] sessionId={}, error={}", sessionId, error.getMessage());
                    emitter.completeWithError(error);
                })
                .start();
        return emitter;
    }
}
