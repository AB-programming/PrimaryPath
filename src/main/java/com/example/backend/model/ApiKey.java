package com.example.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 租户接入凭证。secretHash 是 secret 段的 SHA-256，明文不入库、不进日志 */
@Entity
@Table(name = "api_keys")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_REVOKED = "revoked";

    /** key 明文 pp_<kid>_<secret> 里的 kid，按它查行 */
    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /** 用途备注，如 XX平台生产-1 */
    @Column(nullable = false)
    private String name;

    @Column(name = "secret_hash", nullable = false)
    private String secretHash;

    /** 逗号分隔的 scope，取值见 ApiScope */
    @Column(nullable = false)
    private String scopes;

    /** active / revoked */
    @Column(nullable = false)
    private String status;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** 节流更新，只用于运营排查，不做计费依据 */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}
