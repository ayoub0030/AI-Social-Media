package com.example.blogs.config;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Collections;
import java.util.List;

@Configuration
public class SpringAiConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String baseUrl;
    
    @Value("${spring.ai.openai.chat.options.model:gpt-3.5-turbo}")
    private String model;
    
    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Float temperature;
    
    @Value("${spring.ai.openai.chat.options.max-tokens:500}")
    private Integer maxTokens;

    /**
     * Checks if the API key is valid (not the placeholder)
     */
    private boolean isApiKeyValid() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("sk-placeholder-key-for-compilation-only");
    }

    @Bean
    @ConditionalOnExpression("'${spring.ai.openai.api-key}' != 'sk-placeholder-key-for-compilation-only'")
    public OpenAiApi openAiApi() {
        return new OpenAiApi(apiKey, baseUrl);
    }

    @Bean
    @ConditionalOnExpression("'${spring.ai.openai.api-key}' != 'sk-placeholder-key-for-compilation-only'")
    public OpenAiChatOptions openAiChatOptions() {
        return OpenAiChatOptions.builder()
                .withModel(model)
                .withTemperature(temperature)
                .withMaxTokens(maxTokens)
                .build();
    }

    @Bean
    @Primary
    @ConditionalOnExpression("'${spring.ai.openai.api-key}' != 'sk-placeholder-key-for-compilation-only'")
    public ChatClient openAiChatClient(OpenAiApi openAiApi, OpenAiChatOptions openAiChatOptions) {
        return new OpenAiChatClient(openAiApi, openAiChatOptions);
    }
    
    /**
     * Fallback chat client when API keys are not configured
     */
    @Bean
    @Primary
    @ConditionalOnExpression("'${spring.ai.openai.api-key}' == 'sk-placeholder-key-for-compilation-only'")
    public ChatClient fallbackChatClient() {
        return new ChatClient() {
            @Override
            public ChatResponse call(Prompt prompt) {
                // Create a fallback response
                String response = "API key not configured. This is a fallback response.";
                Generation generation = new Generation(response);
                return new ChatResponse(Collections.singletonList(generation));
            }
            
            @Override
            public ChatResponse call(List<Message> messages) {
                // Create a fallback response
                String response = "API key not configured. This is a fallback response.";
                Generation generation = new Generation(response);
                return new ChatResponse(Collections.singletonList(generation));
            }
        };
    }
}
