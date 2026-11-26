CREATE TABLE IF NOT EXISTS chat_memory (
    memory_id VARCHAR(255) PRIMARY KEY,
    messages  TEXT        NOT NULL DEFAULT '[]',
    updated_at TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sessions (
    id         VARCHAR(255) PRIMARY KEY,
    title      VARCHAR(500) NOT NULL DEFAULT 'New Chat',
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS documents (
    id         VARCHAR(255) PRIMARY KEY,
    name       VARCHAR(500) NOT NULL,
    content    TEXT         NOT NULL,
    chunk_count INTEGER     NOT NULL DEFAULT 0,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    id            VARCHAR(255) PRIMARY KEY,
    username      VARCHAR(32)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(16)  NOT NULL DEFAULT 'USER',   -- USER / ADMIN
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 已有库补 role 列；新库由上面的建表语句直接带上
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(16) NOT NULL DEFAULT 'USER';

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON users (username);

-- 租户：接入方（医院、问诊平台）。方案见 docs/auth-design.md
CREATE TABLE IF NOT EXISTS tenants (
    id         VARCHAR(32)  PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    status     VARCHAR(16)  NOT NULL DEFAULT 'active',   -- active / suspended
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 租户接入凭证。只存 SHA-256(secret)，明文只在发放时返回一次
CREATE TABLE IF NOT EXISTS api_keys (
    id           VARCHAR(16)  PRIMARY KEY,                -- 即 key 里的 kid
    tenant_id    VARCHAR(32)  NOT NULL,
    name         VARCHAR(200) NOT NULL,                   -- 用途备注
    secret_hash  CHAR(64)     NOT NULL,
    scopes       VARCHAR(200) NOT NULL,                   -- 逗号分隔，如 assist,feedback
    status       VARCHAR(16)  NOT NULL DEFAULT 'active',  -- active / revoked
    expires_at   TIMESTAMP,
    last_used_at TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_api_keys_tenant ON api_keys (tenant_id);
