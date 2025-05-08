package com.example.blogs.AIAgents;

import com.example.blogs.annotations.AiAgent;
import com.example.blogs.controllers.FlickController;
import com.example.blogs.dtos.PostDTO;
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
}
