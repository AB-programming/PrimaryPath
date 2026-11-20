package com.example.backend.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时打印当前生效的 LLM 提供方配置（绝不打印 api-key），
 * 便于确认模型切换是否生效、base-url 是否手误。
 */
@Slf4j
@Component
public class LlmConfigLogger implements ApplicationRunner {

    private final String baseUrl;
    private final String modelName;

    public LlmConfigLogger(
            @Value("${ai.llm.base-url}") String baseUrl,
            @Value("${ai.llm.model-name}") String modelName) {
        this.baseUrl = baseUrl;
        this.modelName = modelName;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("[LLM] Active provider: base-url={}, model-name={}", baseUrl, modelName);
    }
}