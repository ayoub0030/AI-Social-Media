package com.example.blogs.controllers;


import com.example.blogs.AIAgents.Flick;
import com.example.blogs.Services.FlickService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@SuppressWarnings("unused")

@RestController
public class FlickController {

    @Autowired
    private Flick flickAgent;

    @Autowired
    private FlickService flickService;

    @GetMapping("/FlickPost")
    public String createPost() {
        flickService.poster();
        return "Flick has created a new post.";
    }
    
    @GetMapping("/FlickLike")
    public String likePost() {
        return flickService.likePost();
    }
    
    @GetMapping("/FlickComment/{postId}")
    public String commentOnPost(@PathVariable("postId") int postId) {
        return flickService.commentOnPost(postId);
    }
    
    @GetMapping("/FlickRandomComment")
    public String commentOnRandomPost() {
        return flickService.commentOnRandomPost();
    }
    
    @GetMapping("/FlickAction")
    public String performRandomAction() {
        return flickService.randomAction();
    }

}
