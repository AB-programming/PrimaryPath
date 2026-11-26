package com.example.backend.security;

import com.example.backend.model.ApiKey;
import com.example.backend.model.Tenant;
import com.example.backend.repository.ApiKeyRepository;
import com.example.backend.repository.TenantRepository;
import com.example.backend.service.ApiKeyService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiKeyAuthInterceptorTest {

    private static final String TENANT_ID = "t_test";

    private ApiKeyRepository apiKeyRepository;
    private TenantRepository tenantRepository;
    private ApiKeyAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        apiKeyRepository = mock(ApiKeyRepository.class);
        tenantRepository = mock(TenantRepository.class);
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> inv.getArgument(0));
        interceptor = new ApiKeyAuthInterceptor(new ApiKeyService(apiKeyRepository, tenantRepository));
    }

    @Test
    @DisplayName("缺少 X-Api-Key 返回 401")
    void missingHeaderRejected() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(new MockHttpServletRequest("GET", "/v1/diseases"), response, null));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":\"UNAUTHORIZED\""));
    }

    @Test
    @DisplayName("格式不对或 secret 不对都按无效处理，401")
    void invalidKeyRejected() throws Exception {
        ApiKeyCodec.Generated generated = stubKey("assist", ApiKey.STATUS_ACTIVE, Tenant.STATUS_ACTIVE);
        String secret = ApiKeyCodec.parse(generated.raw()).secret();
        String tampered = generated.raw().substring(0, generated.raw().length() - 1)
                + (secret.endsWith("A") ? "B" : "A");

        assertEquals(401, reject(generated.raw().substring(0, 10)).getStatus());
        assertEquals(401, reject(tampered).getStatus());
        assertEquals(401, reject(generated.raw().replaceFirst("pp_", "xx_")).getStatus());
    }

    @Test
    @DisplayName("已吊销的 key 401，租户停用 403")
    void revokedOrSuspendedRejected() throws Exception {
        ApiKeyCodec.Generated revoked = stubKey("assist", ApiKey.STATUS_REVOKED, Tenant.STATUS_ACTIVE);
        MockHttpServletResponse revokedResponse = reject(revoked.raw());
        assertEquals(401, revokedResponse.getStatus());
        assertTrue(revokedResponse.getContentAsString().contains("已吊销"));

        ApiKeyCodec.Generated suspended = stubKey("assist", ApiKey.STATUS_ACTIVE, Tenant.STATUS_SUSPENDED);
        MockHttpServletResponse suspendedResponse = reject(suspended.raw());
        assertEquals(403, suspendedResponse.getStatus());
        assertTrue(suspendedResponse.getContentAsString().contains("\"code\":\"FORBIDDEN\""));
    }

    @Test
    @DisplayName("有效 key 通过，注入 tenantId 和 apiKeyId，不需要 scope 的接口放行")
    void validKeyPassesAndInjectsAttributes() throws Exception {
        ApiKeyCodec.Generated generated = stubKey("assist", ApiKey.STATUS_ACTIVE, Tenant.STATUS_ACTIVE);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/diseases");
        request.addHeader(ApiKeyAuthInterceptor.HEADER, generated.raw());

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), null));
        assertEquals(TENANT_ID, request.getAttribute(ApiKeyAuthInterceptor.ATTR_TENANT_ID));
        assertEquals(generated.kid(), request.getAttribute(ApiKeyAuthInterceptor.ATTR_API_KEY_ID));
    }

    @Test
    @DisplayName("scope 不足返回 403")
    void missingScopeRejected() throws Exception {
        ApiKeyCodec.Generated generated = stubKey("feedback", ApiKey.STATUS_ACTIVE, Tenant.STATUS_ACTIVE);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/assist/stream");
        request.addHeader(ApiKeyAuthInterceptor.HEADER, generated.raw());
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, null));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("assist"));
    }

    @Test
    @DisplayName("没登记 scope 的 /v1 路径默认拒绝")
    void unregisteredPathRejected() throws Exception {
        ApiKeyCodec.Generated generated = stubKey("assist", ApiKey.STATUS_ACTIVE, Tenant.STATUS_ACTIVE);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/assist/unknown");
        request.addHeader(ApiKeyAuthInterceptor.HEADER, generated.raw());
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, null));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("接口未开放"));
    }

    @Test
    @DisplayName("last_used_at 节流落库：短时间内多次请求只写一次")
    void lastUsedWriteIsThrottled() throws Exception {
        ApiKeyCodec.Generated generated = stubKey("assist", ApiKey.STATUS_ACTIVE, Tenant.STATUS_ACTIVE);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/diseases");
        request.addHeader(ApiKeyAuthInterceptor.HEADER, generated.raw());

        interceptor.preHandle(request, new MockHttpServletResponse(), null);
        interceptor.preHandle(request, new MockHttpServletResponse(), null);

        verify(apiKeyRepository, times(1)).save(any(ApiKey.class));
    }

    /** 生成一把真 key 并让模拟仓库能查到它，返回明文 */
    private ApiKeyCodec.Generated stubKey(String scopes, String keyStatus, String tenantStatus) {
        ApiKeyCodec.Generated generated = ApiKeyCodec.generate();
        LocalDateTime now = LocalDateTime.now();
        ApiKey key = ApiKey.builder()
                .id(generated.kid())
                .tenantId(TENANT_ID)
                .name("test")
                .secretHash(generated.secretHash())
                .scopes(scopes)
                .status(keyStatus)
                .createdAt(now)
                .build();
        Tenant tenant = Tenant.builder()
                .id(TENANT_ID)
                .name("test")
                .status(tenantStatus)
                .createdAt(now)
                .build();
        when(apiKeyRepository.findById(generated.kid())).thenReturn(Optional.of(key));
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        return generated;
    }

    private MockHttpServletResponse reject(String rawKey) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/diseases");
        request.addHeader(ApiKeyAuthInterceptor.HEADER, rawKey);
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request, response, null));
        return response;
    }
}
