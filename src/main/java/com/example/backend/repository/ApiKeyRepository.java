package com.example.backend.repository;

import com.example.backend.model.ApiKey;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {

    List<ApiKey> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
