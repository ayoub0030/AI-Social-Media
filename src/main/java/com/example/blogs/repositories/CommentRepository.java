package com.example.blogs.repositories;

import com.example.blogs.models.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {
    
    // Find all comments for a specific post
    List<Comment> findByPostIdOrderByCreatedAtDesc(int postId);
    
    // Count comments for a specific post
    int countByPostId(int postId);
    
    // Search comments by content
    @Query("SELECT c FROM Comment c WHERE c.content LIKE %?1%")
    List<Comment> searchComments(String keyword);
}
