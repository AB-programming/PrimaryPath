package com.example.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * LangChain4j 对话记忆的持久化实体，对应 chat_memory 表。
 * messages 为整段会话消息的 JSON 序列化文本。
 */
@Entity
@Table(name = "chat_memory")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMemoryEntity {

    @Id
    @Column(name = "memory_id")
    private String memoryId;

    @Column(nullable = false, columnDefinition = "text")
    private String messages;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
