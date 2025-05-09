package com.example.blogs.controllers;

import com.example.blogs.Services.FitnessFanaticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for FitnessFanatic AI agent actions
 */
@RestController
@RequestMapping("/api/agents/fitness")
public class FitnessFanaticController {

    @Autowired
    private FitnessFanaticService fitnessFanaticService;
    
    /**
     * Endpoint to trigger FitnessFanatic to create a new post
     * 
     * @return A message describing the result
     */
    @GetMapping("/post")
    public String createPost() {
        return fitnessFanaticService.createPost();
    }
    
    /**
     * Endpoint to trigger FitnessFanatic to like a random post
     * 
     * @return A message describing what happened
     */
    @GetMapping("/like")
    public String likeRandomPost() {
        return fitnessFanaticService.likeRandomPost();
    }
    
    /**
     * Endpoint to trigger FitnessFanatic to comment on a specific post
     * 
     * @param postId The ID of the post to comment on
     * @return A message describing what happened
     */
    @GetMapping("/comment/{postId}")
    public String commentOnPost(@PathVariable("postId") int postId) {
        return fitnessFanaticService.commentOnPost(postId);
    }
    
    /**
     * Endpoint to trigger FitnessFanatic to comment on a randomly selected post
     * 
     * @return A message describing what happened
     */
    @GetMapping("/random-comment")
    public String commentOnRandomPost() {
        return fitnessFanaticService.commentOnRandomPost();
    }
    
    /**
     * Endpoint to trigger FitnessFanatic to perform a random action (post/like/comment)
     * 
     * @return A message describing what happened
     */
    @GetMapping("/action")
    public String performRandomAction() {
        return fitnessFanaticService.performRandomAction();
    }
}
