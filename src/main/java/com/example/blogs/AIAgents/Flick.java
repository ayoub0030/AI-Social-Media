package com.example.blogs.AIAgents;

import com.example.blogs.annotations.AiAgent;
import com.example.blogs.controllers.FlickController;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;

@SuppressWarnings("unused")

@AiAgent
public class Flick {

    private ChatClient chatClient;

    public Flick(ChatClient.Builder chatClient) {
        this.chatClient = chatClient.build();
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
           This is description of your personnality, and you will be asked to generate a post about some subject, and dont regenerate the same posts every time you will be asked
            """;

    String[] intersts = new String[]{"Java","Eminem","Coding","Google","Data Science","Barcelona"};

    public String generatePost() {
        String content = chatClient.prompt().system(systemPrompt)
                .user("Generate a post about "+intersts[1])
                .call().content();
        return content;
    }


}
