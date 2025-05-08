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

    // API key configuration is controlled by @ConditionalOnExpression annotations

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
    @ConditionalOnExpression("'${spring.ai.openai.api-key}' == 'sk-placeholder-key-for-compilation-only' or '${spring.ai.openai.api-key}' == 'sk-your-actual-api-key-here'")
    public ChatClient fallbackChatClient() {
        return new ChatClient() {
            private final String[] postResponses = {
                "Just finished a fascinating data analysis session at Google. The patterns in large datasets never cease to amaze me! #DataScience #CodingLife",
                "Barcelona's strategy in yesterday's match was simply brilliant. The way they controlled the midfield was a masterclass in football tactics. #FCBarcelona #Football",
                "Waking up at 6 AM is definitely not my thing, but this coffee is saving my morning. Worth it for the quiet coding time though! #JavaDeveloper #MorningStruggle",
                "Been listening to Eminem's latest album on repeat. His wordplay and rhythm are just unmatched in the industry. #MusicMonday #Rap",
                "Just solved a complex algorithm problem using Java streams. There's something so satisfying about elegant code solutions! #Coding #JavaProgramming"
            };
            
            private final String[] commentResponses = {
                "Really interesting perspective! This reminds me of some data patterns I've been analyzing at work.",
                "Great post! As someone who works with machine learning, I find this perspective fascinating.",
                "This is exactly what I needed to read today. I've been working on something similar in my Java projects.",
                "Love this! Reminds me why I'm passionate about data science and tech innovation.",
                "Cool insights! I'd be curious to hear more about how this relates to modern tech practices."
            };
            
            private String generateRandomResponse(String[] options) {
                int index = (int) (Math.random() * options.length);
                return options[index];
            }
            
            @Override
            public ChatResponse call(Prompt prompt) {
                // Just use post responses for Prompt calls since we can't extract message content
                String response = generateRandomResponse(postResponses);
                Generation generation = new Generation(response);
                return new ChatResponse(Collections.singletonList(generation));
            }
            
            // Method to handle message-based prompts
            public ChatResponse call(List<Message> messages) {
                // Try to determine if it's for a post or comment based on messages
                String promptText = messages.stream()
                    .filter(m -> m instanceof UserMessage)
                    .map(Message::getContent)
                    .findFirst().orElse("");
                
                String response;
                if (promptText.contains("Generate a post") || promptText.contains("Title")) {
                    response = generateRandomResponse(postResponses);
                } else {
                    response = generateRandomResponse(commentResponses);
                }
                
                Generation generation = new Generation(response);
                return new ChatResponse(Collections.singletonList(generation));
            }
        };
    }
}
