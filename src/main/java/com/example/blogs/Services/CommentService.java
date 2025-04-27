package com.example.blogs.Services;

import com.example.blogs.models.Comment;
import com.example.blogs.models.Post;
import com.example.blogs.repositories.CommentRepository;
import com.example.blogs.repositories.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private PostRepository postRepository;

    // Get all comments for a specific post
    public List<Comment> getCommentsByPostId(int postId) {
        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId);
    }
    
    // Get a specific comment by ID
    public Comment getCommentById(int id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));
    }
    
    // Create a new comment for a post
    public Comment createComment(int postId, Comment comment) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));
        
        // If no author is provided, set a default one
        if (comment.getAuthor() == null || comment.getAuthor().isEmpty()) {
            comment.setAuthor("Anonymous");
        }
        
        comment.setPost(post);
        Comment savedComment = commentRepository.save(comment);
        
        // Update the post's comments list
        post.addComment(comment);
        postRepository.save(post);
        
        System.out.println("Comment created for post: " + post.getTitle());
        
        return savedComment;
    }
    
    // Delete a comment
    public void deleteComment(int commentId) {
        Comment comment = getCommentById(commentId);
        Post post = comment.getPost();
        
        // Remove the comment from the post's comments list
        post.removeComment(comment);
        
        // Save the post to update the relationship
        postRepository.save(post);
        
        // Delete the comment
        commentRepository.deleteById(commentId);
        System.out.println("Comment deleted for post: " + post.getTitle());
    }
    
    // Count comments for a post
    public int countCommentsByPostId(int postId) {
        return commentRepository.countByPostId(postId);
    }
}
