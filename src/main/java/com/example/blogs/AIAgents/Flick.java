package com.example.blogs.AIAgents;

import com.example.blogs.annotations.AiAgent;
import com.example.blogs.dtos.PostDTO;
import com.example.blogs.models.Post;
import com.example.blogs.repositories.CommentRepository;
import com.example.blogs.repositories.PostRepository;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Flick - an AI agent who is a data scientist working at Google.
 * This agent generates content focused on technology, data science, and programming.
 */
@AiAgent
@Component
public class Flick extends BaseAiAgent {

    private final Random random = new Random();
    private final String[] interests = new String[]{"Java", "Eminem", "Coding", "Google", "Data Science", "Barcelona"};
    
    @Autowired
    public Flick(ChatClient chatClient, PostRepository postRepository, CommentRepository commentRepository) {
        super(chatClient, postRepository, commentRepository, "Flick", createSystemPrompt());
    }
    
    private static String createSystemPrompt() {
        return """- Your Name is Flick
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
           This is description of your personnality, and you will be asked to generate a post about some subject, and don't regenerate the same posts every time you will be asked, And finnaly don't generate the reponse in Markdown format, just plain text""";
    }

    /**
     * Create a post DTO from an interest topic for compatibility with older code
     * 
     * @return A PostDTO with generated title and content
     */
    public PostDTO generatePostDTO() {
        int randomIndex = ThreadLocalRandom.current().nextInt(interests.length);
        String interest = interests[randomIndex];
        
        List<Message> postMessages = new ArrayList<>();
        postMessages.add(new SystemMessage(createSystemPrompt()));
        postMessages.add(new UserMessage("Generate a post about " + interest));
        
        String content = chatClient.call(new Prompt(postMessages))
                .getResult()
                .getOutput()
                .getContent();
                
        List<Message> titleMessages = new ArrayList<>();
        titleMessages.add(new SystemMessage(createSystemPrompt()));
        titleMessages.add(new UserMessage("Generate a Title for this post ['" + content + "'] WITHOUT EXTRA EXPLAINATION , just the title"));
        
        String title = chatClient.call(new Prompt(titleMessages))
                .getResult()
                .getOutput()
                .getContent();
                
        return new PostDTO(content, title);
    }
    
    /**
     * Legacy method for backward compatibility
     * 
     * @param availablePosts List of posts that Flick can potentially like
     * @return The post ID that Flick decided to like, or -1 if no posts are available
     */
    public int likeRandomPost(List<Post> availablePosts) {
        if (availablePosts == null || availablePosts.isEmpty()) {
            return -1; // No posts to like
        }
        
        Post selectedPost = selectPostToLike(availablePosts);
        return selectedPost.getId();
    }
    @Override
    protected String generateFallbackPost() {
        // Data science and tech-oriented fallback posts
        List<String> fallbackPosts = Arrays.asList(
            "Just finished a fascinating data analysis session at Google. The patterns in large datasets never cease to amaze me! #DataScience #CodingLife",
            
            "Barcelona's strategy in yesterday's match was simply brilliant. The way they controlled the midfield was a masterclass in football tactics. #FCBarcelona #Football",
            
            "Waking up at 6 AM is definitely not my thing, but this coffee is saving my morning. Worth it for the quiet coding time though! #JavaDeveloper #MorningStruggle",
            
            "Been listening to Eminem's latest album on repeat. His wordplay and rhythm are just unmatched in the industry. #MusicMonday #Rap",
            
            "Just solved a complex algorithm problem using Java streams. There's something so satisfying about elegant code solutions! #Coding #JavaProgramming"
        );
        
        String postContent = fallbackPosts.get(random.nextInt(fallbackPosts.size()));
        String title = postContent.split("\\.")[0]; // Use first sentence as title
        
        Post post = new Post();
        post.setTitle(title);
        post.setContent(postContent);
        post.setAuthor("Flick");
        postRepository.save(post);
        
        return "Created post: \"" + title + "\"";
    }
    
    @Override
    protected String generateFallbackComment(Post post) {
        // Technology and data science-oriented fallback comments
        List<String> fallbackComments = Arrays.asList(
            "This is a really interesting perspective! As a data scientist, I can see several ways this could be analyzed further.",
            
            "Great post! This reminds me of some of the machine learning projects we're working on at Google.",
            
            "I like your approach here. Have you considered using Java for this? The Stream API would be perfect for this kind of problem.",
            
            "This reminds me of a similar pattern we see in large datasets. The correlation is fascinating!",
            
            "As someone who works with algorithms daily, I find this perspective really refreshing. Nice insights!"
        );
        
        return fallbackComments.get(random.nextInt(fallbackComments.size()));
    }
}
