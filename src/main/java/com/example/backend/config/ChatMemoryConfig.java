package com.example.backend.config;

import com.example.backend.store.memory.chat.PersistentChatMemoryStore;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfig {

    private final PersistentChatMemoryStore chatMemoryStore;

    public ChatMemoryConfig(PersistentChatMemoryStore chatMemoryStore) {
        this.chatMemoryStore = chatMemoryStore;
    }

    @Bean
    ChatMemoryProvider chatMemoryProvider() {
        Map<Object, ChatMemory> store = new ConcurrentHashMap<>();
        return memoryId -> store.computeIfAbsent(memoryId,
                id -> MessageWindowChatMemory.builder()
                        .id(id)
                        .maxMessages(20)
                        .chatMemoryStore(chatMemoryStore)
                        .build());
    }
}
