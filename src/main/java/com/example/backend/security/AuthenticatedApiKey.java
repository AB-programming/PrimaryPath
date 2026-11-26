package com.example.backend.security;

import com.example.backend.model.ApiKey;
import com.example.backend.model.Tenant;

import java.util.Set;

/** 请求时校验通过的 key 及其租户、scope，由 ApiKeyService 返回给拦截器 */
public record AuthenticatedApiKey(Tenant tenant, ApiKey apiKey, Set<ApiScope> scopes) {
}
