package com.example.backend.controller;

import com.example.backend.dto.CreateApiKeyRequest;
import com.example.backend.dto.CreateTenantRequest;
import com.example.backend.dto.IssuedApiKeyResponse;
import com.example.backend.dto.TenantResponse;
import com.example.backend.dto.UpdateTenantRequest;
import com.example.backend.service.TenantService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端：租户与接入凭证。JWT + ADMIN 角色，由 JwtAuthInterceptor 和 AdminRequiredInterceptor 把关。
 * 接口清单见 PLAN.md「管理端接口」。
 */
@RestController
@RequestMapping("/admin/tenants")
@RequiredArgsConstructor
public class AdminTenantController {

    private final TenantService tenantService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse create(@RequestBody CreateTenantRequest req) {
        return tenantService.create(req);
    }

    @GetMapping
    public List<TenantResponse> list() {
        return tenantService.list();
    }

    @PatchMapping("/{id}")
    public TenantResponse updateStatus(@PathVariable("id") String id,
                                       @RequestBody UpdateTenantRequest req) {
        return tenantService.updateStatus(id, req);
    }

    /** 响应里的 apiKey 是明文，只出现这一次 */
    @PostMapping("/{id}/keys")
    @ResponseStatus(HttpStatus.CREATED)
    public IssuedApiKeyResponse issueKey(@PathVariable("id") String id,
                                         @RequestBody CreateApiKeyRequest req) {
        return tenantService.issueKey(id, req);
    }

    @DeleteMapping("/{id}/keys/{keyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeKey(@PathVariable("id") String id, @PathVariable("keyId") String keyId) {
        tenantService.revokeKey(id, keyId);
    }
}
