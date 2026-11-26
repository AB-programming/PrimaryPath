package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    @Id
    private String id;

    @Column(nullable = false)
    private String username;

    /** BCrypt 哈希，禁止序列化到响应 */
    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** USER / ADMIN。ADMIN 才能进 /admin/**，见 docs/auth-design.md 第 5 节 */
    @Builder.Default
    @Column(nullable = false)
    private String role = ROLE_USER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
