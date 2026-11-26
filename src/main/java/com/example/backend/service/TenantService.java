package com.example.backend.service;

import com.example.backend.dto.ApiKeyResponse;
import com.example.backend.dto.CreateApiKeyRequest;
import com.example.backend.dto.CreateTenantRequest;
import com.example.backend.dto.IssuedApiKeyResponse;
import com.example.backend.dto.TenantResponse;
import com.example.backend.dto.UpdateTenantRequest;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.ApiKey;
import com.example.backend.model.Tenant;
import com.example.backend.repository.ApiKeyRepository;
import com.example.backend.repository.TenantRepository;
import com.example.backend.security.ApiKeyCodec;
import com.example.backend.security.ApiScope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 租户与凭证的管理：建租户、停用、发 key、吊销。请求时的 key 校验在 ApiKeyService。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private static final int NAME_MAX_LENGTH = 200;

    private final TenantRepository tenantRepository;
    private final ApiKeyRepository apiKeyRepository;

    public TenantResponse create(CreateTenantRequest req) {
        Tenant tenant = Tenant.builder()
                .id("t_" + ApiKeyCodec.randomBase62(12))
                .name(requireName(req.name(), "租户名称"))
                .status(Tenant.STATUS_ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        tenantRepository.save(tenant);
        log.info("[Tenant] created id={} name={}", tenant.getId(), tenant.getName());
        return toResponse(tenant, List.of());
    }

    public List<TenantResponse> list() {
        Map<String, List<ApiKey>> keysByTenant = apiKeyRepository.findAll().stream()
                .collect(Collectors.groupingBy(ApiKey::getTenantId));
        return tenantRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(t -> toResponse(t, keysByTenant.getOrDefault(t.getId(), List.of())))
                .toList();
    }

    public TenantResponse updateStatus(String tenantId, UpdateTenantRequest req) {
        Tenant tenant = getTenant(tenantId);
        String status = req.status();
        if (!Tenant.STATUS_ACTIVE.equals(status) && !Tenant.STATUS_SUSPENDED.equals(status)) {
            throw BusinessException.badRequest("status 只能是 active 或 suspended");
        }
        tenant.setStatus(status);
        tenantRepository.save(tenant);
        log.info("[Tenant] status updated id={} status={}", tenantId, status);
        return toResponse(tenant, apiKeyRepository.findByTenantIdOrderByCreatedAtDesc(tenantId));
    }

    /** 发 key。明文只在返回值里出现一次，库里只存哈希 */
    public IssuedApiKeyResponse issueKey(String tenantId, CreateApiKeyRequest req) {
        Tenant tenant = getTenant(tenantId);
        Set<ApiScope> scopes = parseScopes(req.scopes());
        LocalDateTime expiresAt = req.expiresAt();
        if (expiresAt != null && !expiresAt.isAfter(LocalDateTime.now())) {
            throw BusinessException.badRequest("过期时间必须晚于当前时间");
        }

        ApiKeyCodec.Generated generated = ApiKeyCodec.generate();
        ApiKey key = ApiKey.builder()
                .id(generated.kid())
                .tenantId(tenant.getId())
                .name(requireName(req.name(), "凭证名称"))
                .secretHash(generated.secretHash())
                .scopes(ApiScope.join(scopes))
                .status(ApiKey.STATUS_ACTIVE)
                .expiresAt(expiresAt)
                .createdAt(LocalDateTime.now())
                .build();
        apiKeyRepository.save(key);
        log.info("[Tenant] api key issued tenant={} key={} scopes={}",
                tenant.getId(), key.getId(), key.getScopes());
        return new IssuedApiKeyResponse(toKeyResponse(key), generated.raw());
    }

    /** 吊销，重复吊销按幂等处理 */
    public void revokeKey(String tenantId, String keyId) {
        getTenant(tenantId);
        ApiKey key = apiKeyRepository.findById(keyId)
                .filter(k -> k.getTenantId().equals(tenantId))
                .orElseThrow(() -> BusinessException.notFound("凭证不存在"));
        if (ApiKey.STATUS_REVOKED.equals(key.getStatus())) {
            return;
        }
        key.setStatus(ApiKey.STATUS_REVOKED);
        key.setRevokedAt(LocalDateTime.now());
        apiKeyRepository.save(key);
        log.info("[Tenant] api key revoked tenant={} key={}", tenantId, keyId);
    }

    private Tenant getTenant(String id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("租户不存在"));
    }

    private static String requireName(String value, String label) {
        if (value == null || value.isBlank()) {
            throw BusinessException.badRequest(label + "不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.length() > NAME_MAX_LENGTH) {
            throw BusinessException.badRequest(label + "最长 " + NAME_MAX_LENGTH + " 个字符");
        }
        return trimmed;
    }

    private static Set<ApiScope> parseScopes(List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            throw BusinessException.badRequest("scopes 不能为空");
        }
        Set<ApiScope> result = EnumSet.noneOf(ApiScope.class);
        List<String> unknown = new ArrayList<>();
        for (String scope : scopes) {
            ApiScope.fromValue(scope == null ? "" : scope.trim())
                    .ifPresentOrElse(result::add, () -> unknown.add(String.valueOf(scope)));
        }
        if (!unknown.isEmpty()) {
            throw BusinessException.badRequest("不认识的 scope：" + String.join(", ", unknown));
        }
        return result;
    }

    private static TenantResponse toResponse(Tenant tenant, List<ApiKey> keys) {
        return new TenantResponse(tenant.getId(), tenant.getName(), tenant.getStatus(),
                tenant.getCreatedAt(), keys.stream().map(TenantService::toKeyResponse).toList());
    }

    private static ApiKeyResponse toKeyResponse(ApiKey key) {
        return new ApiKeyResponse(key.getId(), key.getName(),
                key.getScopes() == null ? List.of() : List.of(key.getScopes().split(",")),
                key.getStatus(), key.getExpiresAt(), key.getLastUsedAt(), key.getCreatedAt(),
                key.getRevokedAt());
    }
}
