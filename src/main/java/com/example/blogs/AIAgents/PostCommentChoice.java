package com.example.blogs.AIAgents;

import com.example.blogs.models.Post;

/**
 * Holds the result of Flick's post selection and comment generation
 */
public class PostCommentChoice {
    private final Post selectedPost;
    private final String generatedComment;
    
    public PostCommentChoice(Post selectedPost, String generatedComment) {
        this.selectedPost = selectedPost;
        this.generatedComment = generatedComment;
    }
    
    public Post getSelectedPost() {
        return selectedPost;
    }
    
    public String getGeneratedComment() {
        return generatedComment;
    }
}
