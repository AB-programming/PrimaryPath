package com.example.backend.repository;

import com.example.backend.model.SessionInfo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SessionRepository extends JpaRepository<SessionInfo, String> {

    /**
     * 只返回存在对话记忆的会话，按更新时间倒序（对应原 INNER JOIN chat_memory 的语义）。
     */
    @Query("""
            SELECT s FROM SessionInfo s
            WHERE EXISTS (
                SELECT c.memoryId FROM ChatMemoryEntity c WHERE c.memoryId = s.id
            )
            ORDER BY s.updatedAt DESC
            """)
    @Override
    List<SessionInfo> findAll();

    /**
     * 幂等创建会话：已存在时不做任何事（ON CONFLICT DO NOTHING）。
     */
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO sessions (id) VALUES (:id) ON CONFLICT (id) DO NOTHING", nativeQuery = true)
    void createIfNotExists(@Param("id") String id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE sessions SET title = :title, updated_at = CURRENT_TIMESTAMP WHERE id = :id", nativeQuery = true)
    void updateTitle(@Param("id") String id, @Param("title") String title);

    @Modifying
    @Transactional
    @Query(value = "UPDATE sessions SET updated_at = CURRENT_TIMESTAMP WHERE id = :id", nativeQuery = true)
    void touchUpdatedAt(@Param("id") String id);
}
