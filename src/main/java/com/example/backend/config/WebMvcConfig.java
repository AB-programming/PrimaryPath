package com.example.backend.config;

import com.example.backend.security.AdminRequiredInterceptor;
import com.example.backend.security.ApiKeyAuthInterceptor;
import com.example.backend.security.JwtAuthInterceptor;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 两条认证链，互不复用：
 * - /v1/** 走 X-Api-Key（租户接入），健康检查放行；
 * - /auth/me、/admin/** 走 JWT（用户登录），管理端再查库验 ADMIN 角色。
 * /assistant 骨架接口不经过拦截器，保持公开，见 README。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiKeyAuthInterceptor apiKeyAuthInterceptor;
    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final AdminRequiredInterceptor adminRequiredInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyAuthInterceptor)
                .addPathPatterns("/v1/**")
                .excludePathPatterns("/v1/health");

        // 按注册顺序执行：先验 JWT 注入 userId，管理端再查库验角色
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/auth/me", "/admin/**");
        registry.addInterceptor(adminRequiredInterceptor)
                .addPathPatterns("/admin/**");
    }
}
