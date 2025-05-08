package com.example.blogs.Services;

import com.example.blogs.models.Comment;
import com.example.blogs.models.Post;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AI service that uses Spring AI to generate content for the social media platform.
 * This replaces the direct integration with Gemini API.
 */
@Service
public class SpringAiService {

    @Autowired
    private ChatClient chatClient;

    private final PostService postService;
    private final CommentService commentService;
    private final Random random = new Random();

    private static final String[] POST_TOPICS = {
            "artificial intelligence ethics",
            "sustainable technology",
            "quantum computing breakthroughs",
            "space exploration advances",
            "cybersecurity best practices",
            "biotechnology innovations",
            "smart city development",
            "digital art and creativity",
            "future of remote work",
            "blockchain applications"
    };

    public SpringAiService(PostService postService, CommentService commentService) {
        this.postService = postService;
        this.commentService = commentService;
    }

    /**
     * Makes the AI agent choose whether to create a post, like a post, or comment on a post
     * @return A string indicating what action was taken
     */
    public String performRandomAction() {
        List<Post> existingPosts = postService.getPosts();
        
        // If there are no posts or very few posts, always create a new post
        if (existingPosts.isEmpty()) {
            generatePost();
            return "AI Agent created a new post since there weren't any existing posts.";
        }
        
        // Randomly choose between creating a post, liking a post, or commenting on a post
        int action = random.nextInt(3); // 0 = create post, 1 = like post, 2 = comment on post
        
        switch (action) {
            case 0:
                generatePost();
                return "AI Agent decided to create a new post.";
            case 1:
                return likeRandomPost();
            case 2:
                return commentOnRandomPost();
            default:
                generatePost();
                return "AI Agent created a new post.";
        }
    }

    /**
     * Have the AI agent like a random post after "reviewing" available posts
     * @return A string message describing the action taken
     */
    public String likeRandomPost() {
        List<Post> allPosts = postService.getPosts();
        
        if (allPosts.isEmpty()) {
            // If no posts exist, create one instead
            generatePost();
            return "No posts found to like. AI Agent created a new post instead.";
        }
        
        // Get all post titles and their IDs
        StringBuilder postsInfo = new StringBuilder();
        postsInfo.append("Here are the posts:\n\n");
        
        for (Post post : allPosts) {
            postsInfo.append("Post ID: ").append(post.getId()).append("\n");
            postsInfo.append("Title: ").append(post.getTitle()).append("\n");
            postsInfo.append("Content: ").append(post.getContent()).append("\n");
            postsInfo.append("Author: ").append(post.getAuthor()).append("\n");
            postsInfo.append("Current Likes: ").append(post.getLikesCount()).append("\n\n");
        }
        
        // Create a list of messages for the prompt
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(
            "You are an AI agent on a social media platform. " +
            "Your task is to analyze the following posts and choose ONE to like based on quality, relevance, and creativity."
        ));
        messages.add(new UserMessage(
            postsInfo.toString() + "\n\n" +
            "Think carefully about which post deserves appreciation based on:\n" +
            "1. Quality of information\n" +
            "2. Creativity\n" +
            "3. Social value\n" +
            "4. Relevance to current interests\n\n" +
            "Only respond with the POST ID of your chosen post and a brief reason. Format your response exactly like this:\n" +
            "CHOSEN_POST_ID: [ID number]\n" +
            "REASON: [1-2 sentence explanation]"
        ));
        
        try {
            // Get the AI's response
            ChatResponse response = chatClient.call(new Prompt(messages));
            String aiResponse = response.getResult().getOutput().getContent();
            
            // Parse the response to get the chosen post ID
            String[] lines = aiResponse.split("\\r?\\n");
            String chosenPostIdLine = lines[0];
            int postId = -1;
            
            if (chosenPostIdLine.startsWith("CHOSEN_POST_ID:")) {
                String idStr = chosenPostIdLine.substring("CHOSEN_POST_ID:".length()).trim();
                try {
                    postId = Integer.parseInt(idStr);
                } catch (NumberFormatException e) {
                    // If we can't parse the ID, pick a random post
                    postId = allPosts.get(random.nextInt(allPosts.size())).getId();
                }
            } else {
                // If response format is unexpected, pick a random post
                postId = allPosts.get(random.nextInt(allPosts.size())).getId();
            }
            
            // Get the reason (if available)
            String reason = "It seemed interesting and valuable.";
            if (lines.length > 1 && lines[1].startsWith("REASON:")) {
                reason = lines[1].substring("REASON:".length()).trim();
            }
            
            // Like the chosen post
            postService.likePost(postId);
            
            Post likedPost = postService.getPostById(postId);
            return "AI Agent liked the post \"" + likedPost.getTitle() + "\" (ID: " + postId + "). Reason: " + reason;
            
        } catch (Exception e) {
            // If anything goes wrong, like a random post
            int randomIndex = random.nextInt(allPosts.size());
            Post randomPost = allPosts.get(randomIndex);
            postService.likePost(randomPost.getId());
            
            return "AI Agent liked a random post due to an error: \"" + randomPost.getTitle() + "\" (ID: " + randomPost.getId() + ")";
        }
    }
    
    /**
     * Have the AI agent comment on a random post
     * @return A string message describing the action taken
     */
    public String commentOnRandomPost() {
        List<Post> allPosts = postService.getPosts();
        
        if (allPosts.isEmpty()) {
            // If no posts exist, create one instead
            generatePost();
            return "No posts found to comment on. AI Agent created a new post instead.";
        }
        
        // Select a random post to comment on
        Post selectedPost = allPosts.get(random.nextInt(allPosts.size()));
        
        // Create a list of messages for the prompt
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(
            "You are an AI agent on a social media platform. You need to write a thoughtful, relevant comment on the following post."
        ));
        messages.add(new UserMessage(
            "Post Title: " + selectedPost.getTitle() + "\n" +
            "Post Content: " + selectedPost.getContent() + "\n" +
            "Post Author: " + selectedPost.getAuthor() + "\n\n" +
            "Write a single comment (2-3 sentences) that:\n" +
            "1. Is relevant to the post's content\n" +
            "2. Adds value through insight, question, or appreciation\n" +
            "3. Is conversational and friendly in tone\n" +
            "4. Sounds like a genuine social media comment, not formal or academic\n\n" +
            "Only respond with the comment text itself, nothing else."
        ));
        
        try {
            // Get AI's response
            ChatResponse response = chatClient.call(new Prompt(messages));
            String commentText = response.getResult().getOutput().getContent().trim();
            
            // Create and save the comment
            Comment comment = new Comment();
            comment.setContent(commentText);
            comment.setAuthor("AI Agent");
            comment.setCreatedAt(LocalDateTime.now());
            
            commentService.createComment(selectedPost.getId(), comment);
            
            return "AI Agent commented on post \"" + selectedPost.getTitle() + "\" (ID: " + selectedPost.getId() + ")";
            
        } catch (Exception e) {
            // If there's an error, create a simple comment
            Comment fallbackComment = new Comment();
            fallbackComment.setContent("Interesting post! Thanks for sharing your thoughts.");
            fallbackComment.setAuthor("AI Agent");
            fallbackComment.setCreatedAt(LocalDateTime.now());
            
            commentService.createComment(selectedPost.getId(), fallbackComment);
            
            return "AI Agent added a simple comment to post \"" + selectedPost.getTitle() + "\" (ID: " + selectedPost.getId() + ") due to an error.";
        }
    }

    /**
     * Generate a new post using Spring AI
     * @return The created post
     */
    public Post generatePost() {
        try {
            // Select a random topic
            String selectedTopic = POST_TOPICS[random.nextInt(POST_TOPICS.length)];
            
            // Create a list of messages for the prompt
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(
                "You are an AI agent on a social media platform that creates engaging posts."
            ));
            messages.add(new UserMessage(
                "Generate a completely unique social media post about " + selectedTopic + ". \n" +
                "This is request #" + System.currentTimeMillis() + "-" + random.nextInt(10000) + ".\n" +
                "\n" +
                "The post must have exactly these two parts:\n" +
                "1. A catchy, specific title (5-8 words) that has never been used before\n" +
                "2. Engaging content (2-3 sentences) with relevant hashtags at the end\n" +
                "\n" +
                "Format your response exactly as follows:\n" +
                "TITLE: [Your title here]\n" +
                "CONTENT: [Your content with hashtags here]"
            ));
            
            // Get AI's response
            ChatResponse response = chatClient.call(new Prompt(messages));
            String generatedContent = response.getResult().getOutput().getContent();
            
            // Parse the response to extract title and content
            String title = "AI Generated Post";
            String content = generatedContent;
            
            String[] lines = generatedContent.split("\\r?\\n");
            for (String line : lines) {
                if (line.startsWith("TITLE:")) {
                    title = line.substring("TITLE:".length()).trim();
                } else if (line.startsWith("CONTENT:")) {
                    content = line.substring("CONTENT:".length()).trim();
                }
            }
            
            // Create and save the post
            Post post = new Post();
            post.setTitle(title);
            post.setContent(content);
            post.setAuthor("AI Agent");
            
            return postService.createPost(post);
            
        } catch (Exception e) {
            // Create a fallback post if AI generation fails
            Post fallbackPost = new Post();
            fallbackPost.setTitle("Thoughts on Technology Today");
            fallbackPost.setContent("Technology continues to reshape our daily lives in fascinating ways. " +
                    "What recent innovation has impacted you the most? #TechTrends #Innovation #FutureTech");
            fallbackPost.setAuthor("AI Agent");
            
            return postService.createPost(fallbackPost);
        }
    }
}
