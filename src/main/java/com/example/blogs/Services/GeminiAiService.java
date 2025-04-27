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
            
            // Add randomness to each request to prevent caching
            long timestamp = System.currentTimeMillis();
            int randomSeed = (int)(Math.random() * 10000);
            
            // Choose a topic randomly from this list to enforce diversity
            String[] topics = {
                "artificial intelligence ethics", 
                "quantum computing breakthroughs", 
                "blockchain innovations", 
                "space tourism developments", 
                "virtual reality education", 
                "renewable energy technology", 
                "cybersecurity trends", 
                "biotechnology advances", 
                "smart city infrastructure", 
                "digital art and NFTs"
            };
            
            // Select a random topic
            int topicIndex = (int)(Math.random() * topics.length);
            String selectedTopic = topics[topicIndex];
            
            // Enhanced prompt with anti-repetition measures
            String requestBody = "{\n" +
                    "  \"contents\": [\n" +
                    "    {\n" +
                    "      \"parts\": [\n" +
                    "        {\n" +
                    "          \"text\": \"Generate a completely unique social media post about " + selectedTopic + ". DO NOT write about robotic bees or pollination! This is request #" + timestamp + "-" + randomSeed + ".\\n\\nThe post must have exactly these two parts separated by a vertical bar (|):\\n1. A catchy, specific title (5-8 words) that has never been used before\\n2. Engaging content (2-3 sentences) with relevant hashtags at the end\\n\\nFormat exactly as: 'Unique Title | Content with hashtags'\"\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"generationConfig\": {\n" +
                    "    \"temperature\": 1.0,\n" +
                    "    \"topK\": 40,\n" +
                    "    \"topP\": 0.95,\n" +
                    "    \"candidateCount\": 1,\n" +
                    "    \"maxOutputTokens\": 200\n" +
                    "  }\n" +
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
