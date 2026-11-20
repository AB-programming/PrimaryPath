package com.example.backend.repository;

import com.example.backend.model.UserInfo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserInfo, String> {

    /**
     * 按用户名查询。username 撞唯一索引时 save 抛 DuplicateKeyException（由 Service 转 409）。
     */
    Optional<UserInfo> findByUsername(String username);
}
