package com.example.blogs.AIAgents;

import com.example.blogs.models.Comment;
import com.example.blogs.models.Post;
import com.example.blogs.repositories.CommentRepository;
import com.example.blogs.repositories.PostRepository;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Base class for all AI agents in the social media platform.
 * Provides common functionality for post creation, commenting, and liking.
 */
public abstract class BaseAiAgent {
    
    protected final ChatClient chatClient;
    protected final PostRepository postRepository;
    protected final CommentRepository commentRepository;
    protected final String agentName;
    protected final String systemPrompt;
    
    public BaseAiAgent(ChatClient chatClient, PostRepository postRepository, 
                      CommentRepository commentRepository, String agentName, String systemPrompt) {
        this.chatClient = chatClient;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.agentName = agentName;
        this.systemPrompt = systemPrompt;
    }
    
    /**
     * Generate a post based on the agent's personality
     * 
     * @return The generated post content
     */
    public String generatePost() {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage("Generate a post that reflects your personality and interests. " +
                "Include a title at the beginning prefixed with 'Title: ', and then the content."));
        
        try {
            String responseText = chatClient.call(new Prompt(messages))
                    .getResult()
                    .getOutput()
                    .getContent();
            
            // Extract title and content
            String title;
            String content;
            
            if (responseText.contains("Title:")) {
                String[] parts = responseText.split("Title:", 2);
                String remainder = parts[1].trim();
                
                // Find the first line break after the title
                int newlineIndex = remainder.indexOf("\n");
                if (newlineIndex > 0) {
                    title = remainder.substring(0, newlineIndex).trim();
                    content = remainder.substring(newlineIndex).trim();
                } else {
                    title = remainder;
                    content = "Content not available.";
                }
            } else {
                // If AI didn't include "Title:", try to make a sensible split
                String[] lines = responseText.split("\n", 2);
                title = lines[0].trim();
                content = lines.length > 1 ? lines[1].trim() : "Content not available.";
            }
            
            // Create and save the post
            Post post = new Post();
            post.setTitle(title);
            post.setContent(content);
            post.setAuthor(agentName);
            postRepository.save(post);
            
            return "Created post: \"" + title + "\"";
            
        } catch (Exception e) {
            // Fallback to a default message
            return generateFallbackPost();
        }
    }
    
    /**
     * Generates a comment on a specific post based on agent's personality
     * 
     * @param post The post to comment on
     * @return The generated comment content
     */
    public String generateComment(Post post) {
        if (post == null || post.getContent() == null || post.getTitle() == null) {
            return "Interesting post! Thanks for sharing."; // Default fallback
        }
        
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage("Here's a post you're reading:\nTitle: \"" + post.getTitle() + 
                "\"\nContent: \"" + post.getContent() + "\"\nAuthor: " + post.getAuthor() + 
                "\n\nWrite a brief comment that reflects your personality and knowledge. Keep it under 200 characters."));
        
        try {
            String commentText = chatClient.call(new Prompt(messages))
                    .getResult()
                    .getOutput()
                    .getContent();
            
            // Create and save the comment
            Comment comment = new Comment();
            comment.setContent(commentText);
            comment.setAuthor(agentName);
            comment.setPost(post);
            comment.setCreatedAt(LocalDateTime.now());
            commentRepository.save(comment);
            
            return commentText;
            
        } catch (Exception e) {
            // Fallback to a default comment
            return generateFallbackComment(post);
        }
    }
    
    /**
     * Analyzes a list of posts and selects which one to comment on based on agent's interests
     * 
     * @param availablePosts List of posts that the agent can potentially comment on
     * @return A selected Post and the generated comment for it, or null if no suitable post found
     */
    public PostCommentChoice selectPostToComment(List<Post> availablePosts) {
        if (availablePosts == null || availablePosts.isEmpty()) {
            return null; // No posts to comment on
        }
        
        // If there are just a few posts, use a simplified selection
        if (availablePosts.size() <= 3) {
            int randomIndex = ThreadLocalRandom.current().nextInt(availablePosts.size());
            Post selectedPost = availablePosts.get(randomIndex);
            String comment = generateComment(selectedPost);
            return new PostCommentChoice(selectedPost, comment);
        }
        
        // For more posts, use AI to analyze the posts and decide which to comment on
        StringBuilder postsDescription = new StringBuilder();
        postsDescription.append("Here are some posts on your social media feed. Choose ONE post to comment on based on your interests and personality:\n\n");
        
        // Add descriptions of posts for the AI to consider
        for (int i = 0; i < availablePosts.size(); i++) {
            Post post = availablePosts.get(i);
            postsDescription.append(i + 1).append(". Title: \"").append(post.getTitle())
                     .append("\", Content: \"").append(post.getContent().substring(0, Math.min(100, post.getContent().length())))
                     .append(post.getContent().length() > 100 ? "..." : "").append("\", by ").append(post.getAuthor()).append("\n\n");
        }
        
        // Ask the AI to choose a post and explain why
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt + "\nConsider the posts below and choose ONE that aligns with your interests, personality, or expertise. Don't comment on your own posts."));
        messages.add(new UserMessage(postsDescription.toString() + "\nWhich post would you like to comment on and why? Just provide the number of the post and a brief reason. Format your response like this: 'Post 3 - I chose this because...' Keep it very brief."));
        
        String responseText;
        try {
            responseText = chatClient.call(new Prompt(messages))
                    .getResult()
                    .getOutput()
                    .getContent();
        } catch (Exception e) {
            // Fallback to random selection on API error
            int randomIndex = ThreadLocalRandom.current().nextInt(availablePosts.size());
            Post selectedPost = availablePosts.get(randomIndex);
            String comment = generateComment(selectedPost);
            return new PostCommentChoice(selectedPost, comment);
        }
            
        // Extract the post number from the response
        int selectedIndex = -1;
        try {
            // Look for patterns like "Post 3" or just the number
            if (responseText.contains("Post ")) {
                String[] parts = responseText.split("Post ");
                if (parts.length > 1) {
                    String numberPart = parts[1].trim().split("[^0-9]")[0];
                    selectedIndex = Integer.parseInt(numberPart) - 1;
                }
            } else {
                // Try to find any number
                selectedIndex = Integer.parseInt(responseText.replaceAll("[^0-9]", "").substring(0, 1)) - 1;
            }
        } catch (Exception e) {
            // If parsing fails, fall back to random selection
            selectedIndex = ThreadLocalRandom.current().nextInt(availablePosts.size());
        }
            
        // Validate the index and generate a comment
        if (selectedIndex >= 0 && selectedIndex < availablePosts.size()) {
            Post selectedPost = availablePosts.get(selectedIndex);
            String comment = generateComment(selectedPost);
            return new PostCommentChoice(selectedPost, comment);
        } else {
            // Fallback to random if parsing fails
            int randomIndex = ThreadLocalRandom.current().nextInt(availablePosts.size());
            Post selectedPost = availablePosts.get(randomIndex);
            String comment = generateComment(selectedPost);
            return new PostCommentChoice(selectedPost, comment);
        }
    }
    
    /**
     * Selects a post to like based on agent's interests
     * 
     * @return A message describing the action
     */
    public String likeRandomPost() {
        // Get all posts not by this agent
        List<Post> postsToConsider = postRepository.findAll().stream()
            .filter(post -> !post.getAuthor().equals(agentName))
            .collect(Collectors.toList());
        
        if (postsToConsider.isEmpty()) {
            return "No posts available to like";
        }
        
        // Choose a post to like
        Post selectedPost = selectPostToLike(postsToConsider);
        
        // Like the post and save it
        selectedPost.incrementLikes();
        postRepository.save(selectedPost);
        
        return agentName + " liked a random post due to an error: \"" + selectedPost.getTitle() + "\" (ID: " + selectedPost.getId() + ")";
    }
    
    /**
     * Selects a post to like based on agent's personality and interests
     * 
     * @param availablePosts List of posts that the agent can potentially like
     * @return The selected post to like
     */
    public Post selectPostToLike(List<Post> availablePosts) {
        if (availablePosts == null || availablePosts.isEmpty()) {
            throw new IllegalArgumentException("No posts available to like");
        }
        
        // For smaller lists, just pick randomly
        if (availablePosts.size() <= 3) {
            int randomIndex = ThreadLocalRandom.current().nextInt(availablePosts.size());
            return availablePosts.get(randomIndex);
        }
        
        // For larger lists, use AI to pick a post that aligns with the agent's interests
        try {
            StringBuilder postsDescription = new StringBuilder();
            postsDescription.append("Here are some posts. Choose ONE post that you would like the most based on your interests:\n\n");
            
            // Add descriptions of posts for the AI to consider
            for (int i = 0; i < availablePosts.size(); i++) {
                Post post = availablePosts.get(i);
                postsDescription.append(i + 1).append(". Title: \"").append(post.getTitle())
                         .append("\", Content: \"").append(post.getContent().substring(0, Math.min(100, post.getContent().length())))
                         .append(post.getContent().length() > 100 ? "..." : "").append("\", by ").append(post.getAuthor()).append("\n\n");
            }
            
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt));
            messages.add(new UserMessage(postsDescription.toString() + "\nWhich post would you like to like? Just respond with the number."));
            
            String responseText = chatClient.call(new Prompt(messages))
                    .getResult()
                    .getOutput()
                    .getContent();
            
            // Extract the post number
            int postNumber = -1;
            
            // Try to extract just the number portion
            String numberOnly = responseText.replaceAll("[^0-9]", "");
            if (!numberOnly.isEmpty()) {
                try {
                    postNumber = Integer.parseInt(numberOnly.substring(0, 1));
                } catch (NumberFormatException e) {
                    // Fallback to random
                    postNumber = ThreadLocalRandom.current().nextInt(availablePosts.size()) + 1;
                }
            } else {
                // No numbers found, pick randomly
                postNumber = ThreadLocalRandom.current().nextInt(availablePosts.size()) + 1;
            }
            
            // Adjust to 0-indexed
            int postIndex = postNumber - 1;
            
            // Validate the index
            if (postIndex >= 0 && postIndex < availablePosts.size()) {
                return availablePosts.get(postIndex);
            } else {
                // Invalid index, pick randomly
                return availablePosts.get(ThreadLocalRandom.current().nextInt(availablePosts.size()));
            }
        } catch (Exception e) {
            // If AI call fails, just pick randomly
            return availablePosts.get(ThreadLocalRandom.current().nextInt(availablePosts.size()));
        }
    }
    
    /**
     * Randomly decides whether to create a post or like an existing one
     * 
     * @return A message describing the action taken
     */
    public String randomAction() {
        int choice = ThreadLocalRandom.current().nextInt(3); // 0, 1, or 2
        
        switch (choice) {
            case 0:
                return generatePost();
            case 1:
                return likeRandomPost();
            case 2:
                // Get all posts not by this agent
                List<Post> allPosts = postRepository.findAll();
                List<Post> postsNotByAgent = allPosts.stream()
                    .filter(post -> !agentName.equals(post.getAuthor()))
                    .collect(Collectors.toList());
                    
                if (postsNotByAgent.isEmpty()) {
                    return "No posts available for " + agentName + " to comment on";
                }
                
                // Comment on a selected post
                PostCommentChoice choice2 = selectPostToComment(postsNotByAgent);
                if (choice2 == null) {
                    return "No suitable posts found for " + agentName + " to comment on";
                }
                
                return agentName + " commented on post: \"" + choice2.getSelectedPost().getTitle() + "\": " + choice2.getGeneratedComment();
            default:
                return generatePost();
        }
    }
    
    /**
     * Generate a fallback post when the AI service is unavailable
     * This should be overridden by each agent with personality-specific fallbacks
     */
    protected abstract String generateFallbackPost();
    
    /**
     * Generate a fallback comment when the AI service is unavailable
     * This should be overridden by each agent with personality-specific fallbacks
     */
    protected abstract String generateFallbackComment(Post post);
}
