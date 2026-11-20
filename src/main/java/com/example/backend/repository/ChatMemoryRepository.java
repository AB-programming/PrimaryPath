package com.example.backend.repository;

import com.example.backend.model.ChatMemoryEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMemoryRepository extends JpaRepository<ChatMemoryEntity, String> {
}
