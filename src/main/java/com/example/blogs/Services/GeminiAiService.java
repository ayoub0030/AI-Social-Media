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
import java.util.Random;

@Service
public class GeminiAiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final PostService postService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    public GeminiAiService(PostService postService) {
        this.postService = postService;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Make Gemini AI choose whether to create a new post or like an existing post
     * @return A string indicating what action was taken
     */
    public String performRandomAction() {
        // Get all existing posts
        List<Post> existingPosts = postService.getPosts();
        
        // If there are no posts or very few posts, always create a new post
        if (existingPosts.size() < 3) {
            generatePost();
            return "Gemini AI created a new post since there weren't enough existing posts.";
        }
        
        // Randomly choose to either create a post or like a post (50/50 chance)
        boolean shouldCreatePost = random.nextBoolean();
        
        if (shouldCreatePost) {
            generatePost();
            return "Gemini AI decided to create a new post.";
        } else {
            return likeRandomPost();
        }
    }

    /**
     * Have Gemini AI like a random post after "reviewing" available posts
     * @return A string message describing the action taken
     */
    public String likeRandomPost() {
        try {
            // Get all posts
            List<Post> allPosts = postService.getPosts();
            
            if (allPosts.isEmpty()) {
                // If no posts exist, create one instead
                generatePost();
                return "No posts found to like. Gemini AI created a new post instead.";
            }
            
            // Use Gemini to "think about" which post to like
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
            
            // Build a list of post titles and IDs for Gemini to consider
            // But instead of directly embedding them in the JSON, we'll create a proper structure
            StringBuilder postListBuilder = new StringBuilder();
            for (int i = 0; i < allPosts.size(); i++) {
                Post post = allPosts.get(i);
                // Add index and ID without the title to avoid JSON escaping issues
                postListBuilder.append("Post #").append(i + 1).append(": ID ").append(post.getId()).append("\n");
            }
            
            // Create a JSON node structure for the posts using Jackson to properly escape everything
            ObjectMapper mapper = new ObjectMapper();
            
            // Create a JSON array to store post information
            Map<String, Object> requestMap = new HashMap<>();
            Map<String, Object> promptMap = new HashMap<>();
            
            // Construct the prompt with properly escaped post titles
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("Here is a list of posts. Please analyze them and choose one to like:\n\n");
            
            for (int i = 0; i < allPosts.size(); i++) {
                Post post = allPosts.get(i);
                promptBuilder.append("Post #").append(i + 1).append(": ID ").append(post.getId())
                    .append("\nTitle: \"").append(post.getTitle().replace("\"", "\\\"")).append("\"\n\n");
            }
            
            promptBuilder.append("Based on the titles, which post would you like to engage with? Respond with just the post number (e.g., '3').");
            
            // Create the request payload using Jackson for proper JSON escaping
            Map<String, Object> root = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();
            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> part = new HashMap<>();
            
            part.put("text", promptBuilder.toString());
            parts.add(part);
            content.put("parts", parts);
            contents.add(content);
            root.put("contents", contents);
            
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 10);
            root.put("generationConfig", generationConfig);
            
            // Convert to JSON string with proper escaping
            String requestBody = mapper.writeValueAsString(root);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create request entity
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

            // Make the API call
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, requestEntity, String.class);
            String responseBody = response.getBody();
            
            // Parse the response to get the post ID
            JsonNode rootNode = objectMapper.readTree(responseBody);
            String generatedText = rootNode
                .path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText();
            
            System.out.println("Gemini selected: " + generatedText);
            
            // Try to extract post number from Gemini's response
            int selectedPostIndex = -1;
            try {
                selectedPostIndex = Integer.parseInt(generatedText.trim()) - 1;
            } catch (NumberFormatException e) {
                // If Gemini didn't return a valid number, use regex to try to find a number
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+");
                java.util.regex.Matcher matcher = pattern.matcher(generatedText);
                if (matcher.find()) {
                    try {
                        selectedPostIndex = Integer.parseInt(matcher.group()) - 1;
                    } catch (NumberFormatException ex) {
                        // Still couldn't get a valid number, will use random post
                    }
                }
            }
            
            // If we couldn't get a valid index or it's out of bounds, pick a random post
            if (selectedPostIndex < 0 || selectedPostIndex >= allPosts.size()) {
                selectedPostIndex = random.nextInt(allPosts.size());
            }
            
            // Get the selected post and like it
            Post selectedPost = allPosts.get(selectedPostIndex);
            postService.likePost(selectedPost.getId());
            
            return "Gemini AI reviewed the available posts and liked: \"" + selectedPost.getTitle() + "\" (ID: " + selectedPost.getId() + ")";
            
        } catch (Exception e) {
            System.out.println("Error having Gemini like a post: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to just randomly selecting a post to like if there's an error
            try {
                List<Post> allPosts = postService.getPosts();
                if (!allPosts.isEmpty()) {
                    int randomPostIndex = random.nextInt(allPosts.size());
                    Post selectedPost = allPosts.get(randomPostIndex);
                    postService.likePost(selectedPost.getId());
                    return "Gemini AI encountered an issue analyzing posts, but still liked: \"" + selectedPost.getTitle() + "\" (ID: " + selectedPost.getId() + ")";
                }
            } catch (Exception ex) {
                // If even the fallback fails, just return the error message
            }
            
            return "Gemini AI encountered an error while trying to like a post: " + e.getMessage();
        }
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
