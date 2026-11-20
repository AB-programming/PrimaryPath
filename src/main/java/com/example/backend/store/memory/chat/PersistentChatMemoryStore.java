package com.example.backend.store.memory.chat;

import com.example.backend.model.ChatMemoryEntity;
import com.example.backend.repository.ChatMemoryRepository;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PersistentChatMemoryStore implements ChatMemoryStore {

    private final ChatMemoryRepository chatMemoryRepository;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        return chatMemoryRepository.findById(memoryId.toString())
                .map(ChatMemoryEntity::getMessages)
                .map(ChatMessageDeserializer::messagesFromJson)
                .orElseGet(ArrayList::new);
    }

    @Override
    @Transactional
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        log.debug("Persisting {} messages for memoryId={}", messages.size(), memoryId);
        String json = ChatMessageSerializer.messagesToJson(messages);
        ChatMemoryEntity entity = chatMemoryRepository.findById(memoryId.toString())
                .orElseGet(() -> new ChatMemoryEntity());
        entity.setMemoryId(memoryId.toString());
        entity.setMessages(json);
        chatMemoryRepository.save(entity);
    }

    @Override
    @Transactional
    public void deleteMessages(Object memoryId) {
        log.debug("Deleting messages for memoryId={}", memoryId);
        chatMemoryRepository.deleteById(memoryId.toString());
    }
}
