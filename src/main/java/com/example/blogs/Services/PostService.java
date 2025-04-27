package com.example.blogs.Services;
import com.example.blogs.models.Post;
import com.example.blogs.repositories.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class PostService {
    @Autowired
    private PostRepository postRepository;
    
    // Initialize database with some sample data if empty
    public void initializeSampleData() {
        if (postRepository.count() == 0) {
            List<Post> initialPosts = new ArrayList<>();
            
            initialPosts.add(new Post(0, "Social Media Features Overview", "Admin", "This platform allows you to create and share content with other users, follow trending topics, and engage with the community."));
            initialPosts.add(new Post(0, "Welcome to AI Social Media", "System", "Join our growing community of creators and connect with like-minded individuals."));
            initialPosts.add(new Post(0, "Getting Started Guide", "Support Team", "Learn how to create your profile, make your first post, and discover content that matters to you."));
            initialPosts.add(new Post(0, "Privacy and Security Tips", "Security Team", "Protect your account with these essential privacy settings and security best practices."));
            initialPosts.add(new Post(0, "Content Creation Guidelines", "Moderation Team", "Follow these guidelines to ensure your content meets our community standards."));
            
            postRepository.saveAll(initialPosts);
        }
    }

    public List<Post> getPosts() {
        return postRepository.findAll();
    }

    // Method for pagination
    public List<Post> getPaginatedPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findAll(pageable);
        return postPage.getContent();
    }

    public List<Post> searchPosts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getPosts();
        }
        
        return postRepository.searchPosts(keyword.trim());
    }

    public List<Post> searchPaginatedPosts(String keyword, int page, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getPaginatedPosts(page, size);
        }
        
        List<Post> searchResults = searchPosts(keyword);
        
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, searchResults.size());
        
        // Handle invalid indices
        if (startIndex >= searchResults.size() || startIndex < 0) {
            return new ArrayList<>();
        }
        
        return searchResults.subList(startIndex, endIndex);
    }

    public Post getPostById(int id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));
    }

    public Post createPost(Post post) {
        if (post.getAuthor() == null || post.getAuthor().isEmpty()) {
            post.setAuthor("Anonymous");
        }
        Post savedPost = postRepository.save(post);
        System.out.println("Article créé : " + savedPost.getTitle());
        return savedPost;
    }

    public void updatePost(int id, Post updatedPost) {
        Post existingPost = getPostById(id);
        existingPost.setTitle(updatedPost.getTitle());
        existingPost.setAuthor(updatedPost.getAuthor());
        existingPost.setContent(updatedPost.getContent());
        postRepository.save(existingPost);
        System.out.println("Article mis à jour : " + existingPost.getTitle());
    }

    public void deletePost(int id) {
        Post post = getPostById(id);
        postRepository.deleteById(id);
        System.out.println("Article supprimé : " + post.getTitle());
    }
    
    // Method to like a post
    public void likePost(int id) {
        Post post = getPostById(id);
        post.incrementLikes();
        postRepository.save(post);
        System.out.println("Post liked: " + post.getTitle() + " - Total likes: " + post.getLikesCount());
    }
}