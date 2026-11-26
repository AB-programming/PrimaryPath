package com.example.backend.service;

import com.example.backend.exception.BusinessException;
import com.example.backend.model.ApiKey;
import com.example.backend.model.Tenant;
import com.example.backend.repository.ApiKeyRepository;
import com.example.backend.repository.TenantRepository;
import com.example.backend.security.ApiKeyCodec;
import com.example.backend.security.ApiScope;
import com.example.backend.security.AuthenticatedApiKey;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 请求时的 API Key 校验。key 的发放和吊销在 TenantService。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    /** last_used_at 的落库节流窗口，避免逐请求写库 */
    private static final Duration LAST_USED_WRITE_INTERVAL = Duration.ofMinutes(5);

    private final ApiKeyRepository apiKeyRepository;
    private final TenantRepository tenantRepository;

    /**
     * 校验 X-Api-Key 明文。凭证问题抛 401，租户停用抛 403。
     * kid 不存在和 secret 不对统一按无效处理，不区分提示。
     */
    public AuthenticatedApiKey authenticate(String rawKey) {
        ApiKeyCodec.Parsed parsed = ApiKeyCodec.parse(rawKey);
        if (parsed == null) {
            throw BusinessException.unauthorized("API Key 格式无效");
        }
        ApiKey key = apiKeyRepository.findById(parsed.kid())
                .filter(k -> ApiKeyCodec.matches(parsed.secret(), k.getSecretHash()))
                .orElseThrow(() -> BusinessException.unauthorized("API Key 无效"));
        if (!ApiKey.STATUS_ACTIVE.equals(key.getStatus()) || key.getRevokedAt() != null) {
            throw BusinessException.unauthorized("API Key 已吊销");
        }
        if (key.getExpiresAt() != null && !key.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw BusinessException.unauthorized("API Key 已过期");
        }
        Tenant tenant = tenantRepository.findById(key.getTenantId())
                .orElseThrow(() -> BusinessException.unauthorized("API Key 无效"));
        if (!Tenant.STATUS_ACTIVE.equals(tenant.getStatus())) {
            throw BusinessException.forbidden("租户已停用");
        }
        touchLastUsed(key);
        return new AuthenticatedApiKey(tenant, key, ApiScope.parse(key.getScopes()));
    }

    private void touchLastUsed(ApiKey key) {
        LocalDateTime now = LocalDateTime.now();
        if (key.getLastUsedAt() != null
                && key.getLastUsedAt().plus(LAST_USED_WRITE_INTERVAL).isAfter(now)) {
            return;
        }
        key.setLastUsedAt(now);
        apiKeyRepository.save(key);
    }
}
