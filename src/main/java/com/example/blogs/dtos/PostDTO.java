package com.example.blogs.dtos;



public class PostDTO {

    private String content;
    private String title;

    public PostDTO(String content, String title) {
        this.content = content;
        this.title = title;
    }

    public PostDTO(){}

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
