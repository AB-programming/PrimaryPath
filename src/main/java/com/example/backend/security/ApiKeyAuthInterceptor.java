package com.example.backend.security;

import com.example.backend.exception.BusinessException;
import com.example.backend.service.ApiKeyService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 对外接入鉴权：校验 X-Api-Key，再按路径查 ApiScope.ROUTES 检查 scope。
 * 通过后把 tenantId / apiKeyId 注入 request attribute，/v1 的 controller 从这里取租户。
 * 错误响应带 code，对应 PLAN.md 的错误码表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Api-Key";
    public static final String ATTR_TENANT_ID = "tenantId";
    public static final String ATTR_API_KEY_ID = "apiKeyId";

    private final ApiKeyService apiKeyService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        AuthenticatedApiKey auth;
        try {
            auth = apiKeyService.authenticate(request.getHeader(HEADER));
        } catch (BusinessException e) {
            return reject(response, e);
        }

        String method = request.getMethod();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        ApiScope.Route route = ApiScope.routeFor(method, path);
        if (route == null) {
            // 新接口没登记 scope，默认拒绝，登记在 ApiScope.ROUTES
            log.warn("[ApiKey] 路径未登记 scope，拒绝 {} {} tenant={}", method, path,
                    auth.tenant().getId());
            return reject(response, BusinessException.forbidden("接口未开放"));
        }
        if (route.scope() != null && !auth.scopes().contains(route.scope())) {
            log.info("[ApiKey] scope 不足 tenant={} key={} 缺少={} {} {}", auth.tenant().getId(),
                    auth.apiKey().getId(), route.scope().value(), method, path);
            return reject(response, BusinessException.forbidden("API Key 缺少 scope：" + route.scope().value()));
        }

        request.setAttribute(ATTR_TENANT_ID, auth.tenant().getId());
        request.setAttribute(ATTR_API_KEY_ID, auth.apiKey().getId());
        return true;
    }

    private boolean reject(HttpServletResponse response, BusinessException e) throws IOException {
        boolean unauthorized = e.getStatus() == HttpStatus.UNAUTHORIZED;
        response.setStatus(unauthorized ? HttpStatus.UNAUTHORIZED.value() : e.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        // message 都是服务端常量，不含用户输入，直接拼 JSON
        response.getWriter().write("{\"code\":\"" + (unauthorized ? "UNAUTHORIZED" : "FORBIDDEN")
                + "\",\"message\":\"" + e.getMessage() + "\"}");
        return false;
    }
}
