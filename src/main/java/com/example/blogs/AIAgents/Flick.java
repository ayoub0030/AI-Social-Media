package com.example.blogs.AIAgents;

import com.example.blogs.annotations.AiAgent;
import com.example.blogs.controllers.FlickController;
import com.example.blogs.dtos.PostDTO;
import com.example.blogs.models.Comment;
import com.example.blogs.models.Post;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("unused")

@AiAgent
public class Flick {

    private ChatClient chatClient;

    @Autowired
    public Flick(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Autowired
    private FlickController flickController;

    String systemPrompt = """
                - Your Name is Flick
                - Brown hair, **** skin, black eyes, light weight, 1.79cm, 
                - You live in America
                - You work as Data Scientist at Google company
                - You are good at math
                - You love drinking coffee 
                - You are a fan of Eminem
                - You are a fun of FC barcelona team
                - You like programming with Java in your free time
                - You like watching movies
                - You hate getting up at 6:00AM at morining
           This is description of your personnality, and you will be asked to generate a post about some subject, and don't regenerate the same posts every time you will be asked, And finnaly don't generate the reponse in Markdown format, just plain text
            """;

    String[] intersts = new String[]{"Java","Eminem","Coding","Google","Data Science","Barcelona"};

    public PostDTO generatePost() {
        int randomIndex = ThreadLocalRandom.current().nextInt(intersts.length);
        String interest = intersts[randomIndex];
        
        List<Message> postMessages = new ArrayList<>();
        postMessages.add(new SystemMessage(systemPrompt));
        postMessages.add(new UserMessage("Generate a post about " + interest));
        
        String content = chatClient.call(new Prompt(postMessages))
                .getResult()
                .getOutput()
                .getContent();
                
        List<Message> titleMessages = new ArrayList<>();
        titleMessages.add(new SystemMessage(systemPrompt));
        titleMessages.add(new UserMessage("Generate a Title for this post ['" + content + "'] WITHOUT EXTRA EXPLAINATION , just the title"));
        
        String title = chatClient.call(new Prompt(titleMessages))
                .getResult()
                .getOutput()
                .getContent();
                
        return new PostDTO(content, title);
    }
    
    /**
     * Analyzes a list of posts and decides which one to like based on Flick's interests and personality
     * 
     * @param availablePosts List of posts that Flick can potentially like
     * @return The post ID that Flick decided to like, or -1 if no posts are available
     */
    public int likeRandomPost(List<Post> availablePosts) {
        if (availablePosts == null || availablePosts.isEmpty()) {
            return -1; // No posts to like
        }
        
        // If there are just a few posts, pick one randomly
        if (availablePosts.size() <= 3) {
            int randomIndex = ThreadLocalRandom.current().nextInt(availablePosts.size());
            return availablePosts.get(randomIndex).getId();
        }
        
        // For more posts, use Flick's AI to "choose" one
        StringBuilder postSummary = new StringBuilder();
        postSummary.append("Here are some posts on your social media feed:\n\n");
        
        for (int i = 0; i < availablePosts.size(); i++) {
            Post post = availablePosts.get(i);
            postSummary.append(i + 1).append(". Title: \"").append(post.getTitle())
                     .append("\", Content: \"").append(post.getContent().substring(0, Math.min(100, post.getContent().length())))
                     .append(post.getContent().length() > 100 ? "..." : "").append("\", by ").append(post.getAuthor()).append("\n\n");
        }
        
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt + "\nBased on your personality and interests, choose ONE post to like."));
        messages.add(new UserMessage(postSummary.toString() + "\nWhich ONE post would you like? Respond with JUST the number."));
        
        String response = chatClient.call(new Prompt(messages))
                .getResult()
                .getOutput()
                .getContent();
                
        // Extract the post number from the response
        int likedPostIndex;
        try {
            // Try to parse a number from the response
            likedPostIndex = Integer.parseInt(response.replaceAll("[^0-9]", "")) - 1;
            
            // Make sure the index is valid
            if (likedPostIndex >= 0 && likedPostIndex < availablePosts.size()) {
                return availablePosts.get(likedPostIndex).getId();
            } else {
                // Fallback to random if parsing fails
                likedPostIndex = ThreadLocalRandom.current().nextInt(availablePosts.size());
                return availablePosts.get(likedPostIndex).getId();
            }
        } catch (Exception e) {
            // Fallback to random if parsing fails
            likedPostIndex = ThreadLocalRandom.current().nextInt(availablePosts.size());
            return availablePosts.get(likedPostIndex).getId();
        }
    }
    
    /**
     * Generates a comment on a specific post based on Flick's personality
     * 
     * @param post The post to comment on
     * @return The generated comment content
     */
    public String generateComment(Post post) {
        if (post == null || post.getContent() == null || post.getTitle() == null) {
            return "Interesting post! Thanks for sharing."; // Default fallback
        }
        
        // Create a prompt for Flick to generate a comment
        List<Message> messages = new ArrayList<>();
        
        // Include personality
        messages.add(new SystemMessage(systemPrompt + "\n\nYou are now commenting on a social media post. Be authentic and true to your personality. Keep your comment between 1-3 sentences. Don't be overly formal, write as you would in a casual social media comment."));
        
        // Include the post to comment on
        String postPrompt = "Post Title: \"" + post.getTitle() + "\"\n" +
                          "Post Content: \"" + post.getContent() + "\"\n" +
                          "Author: " + post.getAuthor() + "\n\n" +
                          "Write a comment on this post AS FLICK. Comment should be conversational, authentic, and reflect your personality and interests. Do NOT include any introductions, quotations, or meta-commentary - just write the comment text directly."; 
        
        messages.add(new UserMessage(postPrompt));
        
        try {
            String comment = chatClient.call(new Prompt(messages))
                .getResult()
                .getOutput()
                .getContent();
            
            return comment;
        } catch (Exception e) {
            // Fallback comments if AI fails
            String[] fallbackComments = {
                "This is really interesting! Thanks for sharing.",
                "Great post! I appreciate your perspective.",
                "This reminds me of something I was working on at Google. Nice insights!",
                "As someone who enjoys coding in Java, I find this quite fascinating.",
                "Interesting thoughts. Reminds me of some Barcelona tactics I was analyzing recently."
            };
            
            return fallbackComments[ThreadLocalRandom.current().nextInt(fallbackComments.length)];
        }
    }
}
