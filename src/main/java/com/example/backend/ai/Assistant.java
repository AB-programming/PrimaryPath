package com.example.backend.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface Assistant {

    @SystemMessage("""
            You are a helpful assistant with access to tools and a knowledge base.
            When the user asks a question, relevant document content from the knowledge
            base will be automatically provided to you as context.
            Use tools when appropriate: calculator for math, weather for forecasts,
            date/time functions for temporal queries.
            Answer based on the provided context when available, otherwise use your
            general knowledge. Be concise and accurate.
            """)
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);

    @SystemMessage("""
            You are a helpful assistant with access to tools and a knowledge base.
            When the user asks a question, relevant document content from the knowledge
            base will be automatically provided to you as context.
            Use tools when appropriate: calculator for math, weather for forecasts,
            date/time functions for temporal queries.
            Answer based on the provided context when available, otherwise use your
            general knowledge. Be concise and accurate.
            """)
    TokenStream chatStream(@MemoryId String memoryId, @UserMessage String userMessage);
}
