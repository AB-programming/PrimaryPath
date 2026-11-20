package com.example.backend.repository;

import com.example.backend.model.DocumentInfo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DocumentRepository extends JpaRepository<DocumentInfo, String> {

    @Query("SELECT d FROM DocumentInfo d ORDER BY d.createdAt DESC")
    @Override
    List<DocumentInfo> findAll();
}
