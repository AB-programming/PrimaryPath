package com.example.backend.controller;

import com.example.backend.dto.AuthResponse;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.ApiKey;
import com.example.backend.model.UserInfo;
import com.example.backend.repository.ApiKeyRepository;
import com.example.backend.repository.TenantRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.ApiScope;
import com.example.backend.security.AuthenticatedApiKey;
import com.example.backend.security.JwtTokenProvider;
import com.example.backend.service.ApiKeyService;
import com.example.backend.service.AuthService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理端接口的完整链路：JWT → ADMIN 角色校验 → 租户与凭证 → 用发出的 key 过 ApiKeyService。
 * 需要本机 PG，测试数据在 AfterEach 里清理。
 */
@SpringBootTest
class AdminTenantApiTests {

    @Autowired
    private WebApplicationContext wac;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private ApiKeyRepository apiKeyRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private AuthService authService;
    @Autowired
    private ApiKeyService apiKeyService;
    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private final List<String> createdUserIds = new ArrayList<>();
    private final List<String> createdTenantIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @AfterEach
    void cleanUp() {
        for (String tenantId : createdTenantIds) {
            apiKeyRepository.deleteAll(apiKeyRepository.findByTenantIdOrderByCreatedAtDesc(tenantId));
        }
        tenantRepository.deleteAllById(createdTenantIds);
        userRepository.deleteAllById(createdUserIds);
    }

    @Test
    @DisplayName("租户和 key 的完整生命周期：建租户、发 key、校验、停用、吊销")
    void tenantAndKeyLifecycle() throws Exception {
        String token = adminToken();

        MvcResult created = mockMvc.perform(post("/admin/tenants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"集成测试租户\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode tenant = objectMapper.readTree(body(created));
        String tenantId = tenant.get("id").asText();
        createdTenantIds.add(tenantId);
        assertEquals("active", tenant.get("status").asText());

        MvcResult issuedResult = mockMvc.perform(post("/admin/tenants/" + tenantId + "/keys")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"k1\",\"scopes\":[\"assist\",\"feedback\"]}"))
                .andExpect(status().isCreated())
                .andReturn();
        String issuedBody = body(issuedResult);
        JsonNode issued = objectMapper.readTree(issuedBody);
        String rawKey = issued.get("apiKey").asText();
        String keyId = issued.get("key").get("id").asText();
        assertTrue(rawKey.startsWith("pp_"), rawKey);
        assertFalse(issuedBody.contains("secretHash"), "响应里不能带哈希");

        AuthenticatedApiKey auth = apiKeyService.authenticate(rawKey);
        assertEquals(tenantId, auth.tenant().getId());
        assertEquals(EnumSet.of(ApiScope.ASSIST, ApiScope.FEEDBACK), auth.scopes());

        MvcResult list = mockMvc.perform(get("/admin/tenants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(body(list).contains(keyId));

        mockMvc.perform(patch("/admin/tenants/" + tenantId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"suspended\"}"))
                .andExpect(status().isOk());
        BusinessException suspended = assertThrows(BusinessException.class,
                () -> apiKeyService.authenticate(rawKey));
        assertEquals(HttpStatus.FORBIDDEN, suspended.getStatus());

        mockMvc.perform(patch("/admin/tenants/" + tenantId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"active\"}"))
                .andExpect(status().isOk());
        apiKeyService.authenticate(rawKey);

        mockMvc.perform(delete("/admin/tenants/" + tenantId + "/keys/" + keyId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
        BusinessException revoked = assertThrows(BusinessException.class,
                () -> apiKeyService.authenticate(rawKey));
        assertEquals(HttpStatus.UNAUTHORIZED, revoked.getStatus());

        // 重复吊销幂等
        mockMvc.perform(delete("/admin/tenants/" + tenantId + "/keys/" + keyId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        ApiKey stored = apiKeyRepository.findById(keyId).orElseThrow();
        assertEquals(64, stored.getSecretHash().length());
        assertFalse(rawKey.contains(stored.getSecretHash()));
        assertEquals(ApiKey.STATUS_REVOKED, stored.getStatus());
    }

    @Test
    @DisplayName("没带 token 401；注册出来的普通用户 403")
    void nonAdminIsRejected() throws Exception {
        MvcResult anonymous = mockMvc.perform(get("/admin/tenants")).andReturn();
        assertEquals(401, anonymous.getResponse().getStatus());

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AuthResponse registered = authService.register(new RegisterRequest("it_user_" + suffix, "password123"));
        createdUserIds.add(registered.user().getId());

        MvcResult userCall = mockMvc.perform(get("/admin/tenants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + registered.token()))
                .andReturn();
        assertEquals(403, userCall.getResponse().getStatus());
        assertTrue(body(userCall).contains("管理员"));
    }

    @Test
    @DisplayName("不认识的 scope 返回 400")
    void invalidScopeRejected() throws Exception {
        String token = adminToken();
        MvcResult created = mockMvc.perform(post("/admin/tenants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"scope 测试租户\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String tenantId = objectMapper.readTree(body(created)).get("id").asText();
        createdTenantIds.add(tenantId);

        MvcResult bad = mockMvc.perform(post("/admin/tenants/" + tenantId + "/keys")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"k1\",\"scopes\":[\"assist\",\"bogus\"]}"))
                .andReturn();
        assertEquals(400, bad.getResponse().getStatus());
        assertTrue(body(bad).contains("bogus"));
    }

    /** 建一个 ADMIN 用户并签 JWT */
    private String adminToken() {
        UserInfo admin = UserInfo.builder()
                .id(UUID.randomUUID().toString())
                .username("it_admin_" + UUID.randomUUID().toString().substring(0, 8))
                .passwordHash("unused")
                .role(UserInfo.ROLE_ADMIN)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(admin);
        createdUserIds.add(admin.getId());
        return jwtTokenProvider.generateToken(admin);
    }

    private static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
