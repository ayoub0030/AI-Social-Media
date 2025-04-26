package com.example.blogs.Services;

import com.example.blogs.models.Post;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiAiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final PostService postService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiAiService(PostService postService) {
        this.postService = postService;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public Post generatePost() {
        try {
            // Updated API URL with correct model name
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
            
            System.out.println("Using API URL: " + apiUrl);
            
            // Simplified request payload
            String requestBody = "{\n" +
                    "  \"contents\": [\n" +
                    "    {\n" +
                    "      \"parts\": [\n" +
                    "        {\n" +
                    "          \"text\": \"Generate a short social media post about technology or AI make it funy . The post should be informative and engaging. Format your response with a title followed by a vertical bar (|) and then the content. For example: 'Exciting New AI Developments | Here are some recent breakthroughs...'\"\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create request entity
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

            // Make the API call
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, requestEntity, String.class);
            String responseBody = response.getBody();
            
            System.out.println("Gemini API Response: " + responseBody);

            // Parse the response using Jackson (proper JSON parsing)
            JsonNode rootNode = objectMapper.readTree(responseBody);
            
            // Extract the text field from the nested structure
            String generatedText = rootNode
                .path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText();
            
            System.out.println("Extracted text: " + generatedText);

            // If we successfully got text, create a post
            if (generatedText != null && !generatedText.isEmpty()) {
                // Split the response into title and content
                String[] parts = generatedText.split("\\|", 2);
                String title = parts.length > 1 ? parts[0].trim() : "AI Generated Post";
                String content = parts.length > 1 ? parts[1].trim() : generatedText.trim();

                // Create and save the post
                Post post = new Post();
                post.setTitle(title);
                post.setContent(content);
                post.setAuthor("Gemini AI");
                
                return postService.createPost(post);
            } else {
                throw new Exception("Failed to extract generated text from API response: " + responseBody);
            }
        } catch (Exception e) {
            System.out.println("Error generating AI post: " + e.getMessage());
            e.printStackTrace();
            
            // Create a fallback post if API call fails
            Post post = new Post();
            post.setTitle("AI Post Generation Failed");
            post.setContent("We couldn't generate an AI post at this time. Error: " + e.getMessage());
            post.setAuthor("System");
            return postService.createPost(post);
        }
    }
}
