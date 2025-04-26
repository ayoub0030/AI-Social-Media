package com.example.blogs.Services;

import com.example.blogs.models.Post;
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

    public GeminiAiService(PostService postService) {
        this.postService = postService;
        this.restTemplate = new RestTemplate();
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
                    "          \"text\": \"Generate a short social media post about technology or AI. The post should be informative and engaging. Format your response with a title followed by a vertical bar (|) and then the content. For example: 'Exciting New AI Developments | Here are some recent breakthroughs...'\"\n" +
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

            // Parse the response to extract the generated text
            // This is a simplified parsing - just looking for the text field
            String generatedText = "";
            if (responseBody != null && responseBody.contains("\"text\"")) {
                int startIndex = responseBody.indexOf("\"text\"") + 7;
                int endIndex = responseBody.indexOf("\"", startIndex + 1);
                if (startIndex > 0 && endIndex > 0) {
                    generatedText = responseBody.substring(startIndex + 1, endIndex);
                } else {
                    // Alternative parsing if the structure is different
                    startIndex = responseBody.indexOf("\"content\"");
                    if (startIndex > 0) {
                        startIndex = responseBody.indexOf("\"text\"", startIndex);
                        if (startIndex > 0) {
                            startIndex += 7; // "text":"
                            endIndex = responseBody.indexOf("\"", startIndex + 1);
                            generatedText = responseBody.substring(startIndex + 1, endIndex);
                        }
                    }
                }
            }

            // If we successfully got text, create a post
            if (!generatedText.isEmpty()) {
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
