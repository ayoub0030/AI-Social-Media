package com.example.blogs.controllers;


import com.example.blogs.AIAgents.Flick;
import com.example.blogs.Services.FlickService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@SuppressWarnings("unused")

@RestController
public class FlickController {

    @Autowired
    private Flick flickAgent;

    @Autowired
    private FlickService flickService;

    @GetMapping("/FlickAction")
    public void generatePost() {
        flickService.poster();
    }

}
