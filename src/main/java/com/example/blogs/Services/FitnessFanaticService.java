package com.example.blogs.Services;

import com.example.blogs.AIAgents.FitnessFanatic;
import com.example.blogs.AIAgents.PostCommentChoice;
import com.example.blogs.models.Comment;
import com.example.blogs.models.Post;
import com.example.blogs.repositories.CommentRepository;
import com.example.blogs.repositories.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing the FitnessFanatic AI agent actions
 */
@Service
public class FitnessFanaticService {

    @Autowired
    private FitnessFanatic fitnessFanatic;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    /**
     * Creates a new post from the FitnessFanatic agent
     * 
     * @return A message describing the result
     */
    public String createPost() {
        return fitnessFanatic.generatePost();
    }
    
    /**
     * Makes FitnessFanatic like a random post
     * 
     * @return A message describing what happened
     */
    public String likeRandomPost() {
        return fitnessFanatic.likeRandomPost();
    }
    
    /**
     * Makes FitnessFanatic comment on a specific post
     * 
     * @param postId The ID of the post to comment on
     * @return A message describing what happened
     */
    public String commentOnPost(int postId) {
        Optional<Post> postOptional = postRepository.findById(postId);
        
        if (postOptional.isEmpty()) {
            return "Post not found with ID: " + postId;
        }
        
        Post post = postOptional.get();
        String commentContent = fitnessFanatic.generateComment(post);
        
        Comment comment = new Comment();
        comment.setContent(commentContent);
        comment.setAuthor("FitnessFanatic");
        comment.setPost(post);
        comment.setCreatedAt(LocalDateTime.now());
        
        commentRepository.save(comment);
        
        return "FitnessFanatic commented on post: \"" + post.getTitle() + "\": " + commentContent;
    }
    
    /**
     * Makes FitnessFanatic intelligently select and comment on a post
     * 
     * @return A message describing what happened
     */
    public String commentOnRandomPost() {
        List<Post> allPosts = postRepository.findAll();
        List<Post> postsNotByFitnessFanatic = allPosts.stream()
            .filter(post -> !"FitnessFanatic".equals(post.getAuthor()))
            .collect(Collectors.toList());
            
        if (postsNotByFitnessFanatic.isEmpty()) {
            return "There are no posts for FitnessFanatic to comment on.";
        }
        
        // Let FitnessFanatic analyze and select a post to comment on
        PostCommentChoice choice = fitnessFanatic.selectPostToComment(postsNotByFitnessFanatic);
        
        if (choice == null) {
            return "FitnessFanatic couldn't find a suitable post to comment on.";
        }
        
        // Create and save the comment
        Post selectedPost = choice.getSelectedPost();
        String commentContent = choice.getGeneratedComment();
        
        Comment comment = new Comment();
        comment.setContent(commentContent);
        comment.setAuthor("FitnessFanatic");
        comment.setPost(selectedPost);
        comment.setCreatedAt(LocalDateTime.now());
        
        commentRepository.save(comment);
        
        return "FitnessFanatic selected and commented on post: \"" + selectedPost.getTitle() + "\": " + commentContent;
    }
    
    /**
     * Makes FitnessFanatic perform a random action (post, like, or comment)
     * 
     * @return A message describing what happened
     */
    public String performRandomAction() {
        return fitnessFanatic.randomAction();
    }
}
