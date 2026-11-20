package com.example.backend.controller;

import com.example.backend.dto.HistoryResponse;
import com.example.backend.service.HistoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/sessions/{sessionId}/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping
    public HistoryResponse getHistory(@PathVariable String sessionId) {
        return historyService.getHistory(sessionId);
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportHistory(@PathVariable String sessionId) {
        String text = historyService.exportAsText(sessionId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=chat-" + sessionId + ".txt")
                .body(text);
    }
}
