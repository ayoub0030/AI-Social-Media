package com.example.blogs;

import com.example.blogs.controllers.FlickController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class BlogsApplication {

	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(BlogsApplication.class, args);

		// Get the bean from Spring context instead of using "new"
		FlickController flickController = context.getBean(FlickController.class);

		// Run your task in a separate thread
		new Thread(() -> {
			while (true) {
				flickController.generatePost();
				try {
					Thread.sleep(60000); // Wait for 1 minute
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}).start();
	}
}
