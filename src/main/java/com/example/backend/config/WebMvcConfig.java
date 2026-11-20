package com.example.backend.config;

import com.example.backend.security.JwtAuthInterceptor;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 仅保护需要登录的接口。现有 /assistant、/sessions、/documents 等接口不经过拦截器，保持公开。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 后续要保护其他接口，往 addPathPatterns 追加路径即可
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/auth/me");
    }
}
