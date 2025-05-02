package com.example.blogs.Services;


import com.example.blogs.AIAgents.Flick;
import com.example.blogs.controllers.FlickController;
import com.example.blogs.dtos.PostDTO;
import com.example.blogs.models.Post;
import com.example.blogs.repositories.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@SuppressWarnings("unused")

@Service
public class FlickService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private Flick flick;

    public void poster() {
        Post post1 = new Post();
        PostDTO postDTO = flick.generatePost();
        post1.setAuthor("Flick");
        post1.setContent(postDTO.getContent());
        post1.setTitle(postDTO.getTitle());
        post1.setLikesCount(0);
        postRepository.save(post1);
    }


}
