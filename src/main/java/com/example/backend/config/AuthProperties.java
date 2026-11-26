package com.example.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** application.yml 里 auth.* 的配置 */
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
        /** 是否开放 /auth/register。对外部署改 false，账号由 ADMIN 创建 */
        @DefaultValue("true") boolean openRegistration) {
}
