package com.example.blogs.controllers;
import com.example.blogs.Services.GeminiAiService;
import com.example.blogs.Services.PostService;
import com.example.blogs.Services.CommentService;
import com.example.blogs.models.Post;
import com.example.blogs.models.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Controller
public class PostController {
    @Autowired
    private PostService postService;
    
    @Autowired
    private GeminiAiService geminiAiService;
    
    @Autowired
    private CommentService commentService;
    
    @PostConstruct
    public void init() {
        // Initialize sample data at startup
        postService.initializeSampleData();
    }

    // You have a duplicate mapping, removing the first one
    @GetMapping("/posts")
    public String getAllPosts(Model model,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "6") int size,
                              @RequestParam(required = false) String keyword) {

        List<Post> paginatedPosts;
        int totalPosts;

        if (keyword != null && !keyword.trim().isEmpty()) {
            // Search with pagination
            paginatedPosts = postService.searchPaginatedPosts(keyword, page, size);
            totalPosts = postService.searchPosts(keyword).size();
        } else {
            // Normal pagination without search
            paginatedPosts = postService.getPaginatedPosts(page, size);
            totalPosts = postService.getPosts().size();
        }

        int totalPages = (int) Math.ceil((double) totalPosts / size);

        model.addAttribute("posts", paginatedPosts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalPosts);
        model.addAttribute("pageSize", size);
        model.addAttribute("keyword", keyword); // Add the keyword to preserve it between page changes

        return "posts";
    }

    // Rest of your controller methods remain the same
    @GetMapping("/posts/new")
    public String showNewPostForm(Model model) {
        Post post = new Post();
        model.addAttribute("post", post);
        return "post-form";
    }

    @PostMapping("/posts")
    public String createPost(@ModelAttribute("post") Post post) {
        postService.createPost(post);
        return "redirect:/posts";
    }

    @GetMapping("/posts/edit/{id}")
    public String showEditPostForm(@PathVariable int id, Model model) {
        Post post = postService.getPostById(id);
        model.addAttribute("post", post);
        return "post-form";
    }

    @PostMapping("/posts/{id}")
    public String updatePost(@PathVariable int id, @ModelAttribute("post") Post post) {
        postService.updatePost(id, post);
        return "redirect:/posts";
    }

    @GetMapping("/posts/view/{id}")
    public String viewPost(@PathVariable int id, Model model, 
                          @RequestParam(required = false) String fragment) {
        Post post = postService.getPostById(id);
        model.addAttribute("post", post);
        
        // Add comment functionality
        model.addAttribute("comment", new Comment());
        model.addAttribute("comments", commentService.getCommentsByPostId(id));
        
        return "post-view";
    }

    @GetMapping("/posts/delete/{id}")
    public String deletePost(@PathVariable int id) {
        postService.deletePost(id);
        return "redirect:/posts";
    }
    
    // New endpoint for generating an AI post
    @GetMapping("/posts/generate-ai")
    public String generateAiPost() {
        geminiAiService.generatePost();
        return "redirect:/posts";
    }
    
    // New endpoint for having Gemini AI perform a random action (create post or like post)
    @GetMapping("/posts/ai-action")
    public String performAiAction(Model model) {
        String actionResult = geminiAiService.performRandomAction();
        model.addAttribute("aiActionResult", actionResult);
        return "redirect:/posts?aiAction=" + java.net.URLEncoder.encode(actionResult, java.nio.charset.StandardCharsets.UTF_8);
    }
    
    // New endpoint for liking a post
    @GetMapping("/posts/like/{id}")
    public String likePost(@PathVariable int id, @RequestParam(required = false) String returnUrl) {
        postService.likePost(id);
        
        // If returnUrl is specified, redirect back to that URL
        if (returnUrl != null && !returnUrl.isEmpty()) {
            if (returnUrl.equals("view")) {
                return "redirect:/posts/view/" + id;
            }
        }
        
        // Default redirect to posts page
        return "redirect:/posts";
    }
    
    // Add comment to a post
    @PostMapping("/posts/{id}/comments")
    public String addComment(@PathVariable int id, @ModelAttribute Comment comment) {
        commentService.createComment(id, comment);
        return "redirect:/posts/view/" + id;
    }
    
    // Delete a comment
    @GetMapping("/comments/delete/{id}")
    public String deleteComment(@PathVariable int id) {
        Comment comment = commentService.getCommentById(id);
        int postId = comment.getPost().getId();
        commentService.deleteComment(id);
        return "redirect:/posts/view/" + postId;
    }
}