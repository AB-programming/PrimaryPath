package com.example.backend.controller;

import com.example.backend.model.SessionInfo;
import com.example.backend.service.SessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    public List<SessionInfo> listSessions() {
        return sessionService.listSessions();
    }

    @GetMapping("/{id}")
    public SessionInfo getSession(@PathVariable String id) {
        return sessionService.requireSession(id);
    }

    @PostMapping
    public SessionInfo createSession(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "New Chat");
        return sessionService.createSession(title);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable String id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
