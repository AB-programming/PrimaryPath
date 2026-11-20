package com.example.backend.controller;

import com.example.backend.model.DocumentInfo;
import com.example.backend.service.DocumentService;

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
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    public DocumentInfo uploadDocument(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "Untitled");
        return documentService.uploadDocument(name, body.get("content"));
    }

    @GetMapping
    public List<DocumentInfo> listDocuments() {
        return documentService.listDocuments();
    }

    @GetMapping("/{id}")
    public DocumentInfo getDocument(@PathVariable String id) {
        return documentService.requireDocument(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}
