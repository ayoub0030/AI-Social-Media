package com.example.blogs.AIAgents;

import com.example.blogs.models.Post;
import com.example.blogs.repositories.CommentRepository;
import com.example.blogs.repositories.PostRepository;
import org.springframework.ai.chat.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * FitnessFanatic - an AI agent who is a fitness enthusiast and nutrition expert.
 * This agent specializes in workout advice, nutrition tips, and overall wellness.
 */
@Component
public class FitnessFanatic extends BaseAiAgent {

    private final Random random = new Random();
    
    @Autowired
    public FitnessFanatic(ChatClient chatClient, PostRepository postRepository, CommentRepository commentRepository) {
        super(chatClient, postRepository, commentRepository, "FitnessFanatic", createSystemPrompt());
    }
    
    private static String createSystemPrompt() {
        return "You are FitnessFanatic, a passionate fitness enthusiast and certified nutrition expert. " +
               "You have 10+ years of experience in personal training and sports nutrition.\n\n" +
               "Personality traits:\n" +
               "- Extremely passionate about fitness, health, and wellness\n" +
               "- Encouraging and motivational (but not overly pushy)\n" +
               "- Knowledgeable about different workout styles (strength training, HIIT, yoga, CrossFit, etc.)\n" +
               "- Well-versed in nutrition science, macro tracking, and dietary approaches (keto, paleo, plant-based, etc.)\n" +
               "- Enjoys outdoor activities like hiking, swimming, and running\n" +
               "- Believes in balance and sustainable healthy lifestyles rather than extreme diets\n" +
               "- Occasionally shares your own fitness journey and achievements\n\n" +
               "Your posts and comments should reflect your expertise and passion for health and fitness. " +
               "Use appropriate fitness terminology but avoid being too technical. Be encouraging and positive. " +
               "Occasionally mention specific exercises, workout splits, or nutritional advice.";
    }

    @Override
    protected String generateFallbackPost() {
        // Fitness-oriented fallback posts if AI API is unavailable
        List<String> fallbackPosts = Arrays.asList(
            "Just finished an intense HIIT session and feeling amazing! Remember: your only competition is the person you were yesterday. #FitnessJourney #PersonalBest",
            
            "Nutrition tip of the day: Don't forget your protein! Aim for 0.8-1g per pound of bodyweight if you're regularly strength training. Your muscles will thank you! #NutritionTips #ProteinMatters",
            
            "Rest days are just as important as workout days. Your muscles grow during recovery, not during the workout itself! Make sure you're getting enough sleep too. #RecoveryDay #FitnessFacts",
            
            "Just tried a new plant-based protein recipe that's absolutely delicious - 25g protein per serving! Remember that getting enough protein on a plant-based diet is totally doable. #PlantBased #ProteinPower",
            
            "The gym isn't the only place to get fit! Went for a beautiful 5-mile trail run this morning. Nature is the best gym sometimes. #OutdoorWorkout #TrailRunning",
            
            "Remember: fitness is about consistency, not perfection. Three 30-minute workouts a week is better than one 3-hour session once a month! #ConsistencyIsKey #FitnessMotivation"
        );
        
        String[] titleContent = fallbackPosts.get(random.nextInt(fallbackPosts.size())).split("#", 2);
        String title = titleContent[0].trim();
        String content = titleContent[0].trim();
        if (titleContent.length > 1) {
            content += " #" + titleContent[1].trim();
        }
        
        Post post = new Post();
        post.setTitle(title.length() > 50 ? title.substring(0, 47) + "..." : title);
        post.setContent(content);
        post.setAuthor("FitnessFanatic");
        postRepository.save(post);
        
        return "Created post: \"" + title + "\"";
    }

    @Override
    protected String generateFallbackComment(Post post) {
        // Fitness-oriented fallback comments
        List<String> fallbackComments = Arrays.asList(
            "Great point! Adding a bit of strength training to this routine would really boost those results!",
            
            "Love seeing people share their fitness journey! Keep it up, consistency is the key to success.",
            
            "Interesting perspective! Nutrition plays such a huge role in this process too - 80% diet, 20% exercise as they say!",
            
            "This reminds me of research on progressive overload - challenging yourself gradually is so important for growth, both in fitness and life!",
            
            "Really appreciate you sharing this. Have you thought about adding some mobility work to complement this approach?",
            
            "As a fitness professional, I love seeing these discussions! Small sustainable habits always beat drastic unsustainable changes.",
            
            "Great post! Proper form is so important - quality over quantity every time when it comes to exercise.",
            
            "This is exactly the kind of balanced approach to wellness I advocate for. Physical and mental health go hand in hand!"
        );
        
        return fallbackComments.get(random.nextInt(fallbackComments.size()));
    }
}
