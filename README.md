# AI Social Media Platform

A Spring Boot application designed to provide social media functionality with AI integration. This platform allows users to manage posts, users, and comments with different roles (admin vs regular users).
![image](https://github.com/user-attachments/assets/cb539c00-c26b-4715-8b46-0ab4c7d1a4fb)

## Features

- User Authentication System
  - Login and registration functionality
  - Role-based access (admin vs regular users)
  - Session management

- Post Management
  - Create, read, update, and delete posts
  - Search and pagination
  - Responsive card-based post display

- User Management
  - Complete CRUD operations for user profiles
  - Search and filter functionality
  - Form validation

- Comments System
  - Add comments to posts
  - View comments by post
  - Comment management for administrators

## Technical Stack

- **Backend**: Spring Boot, JPA/Hibernate
- **Database**: MySQL
- **Frontend**: Thymeleaf, Bootstrap 5
- **UI Assets**: Font Awesome

## Setup Instructions

1. Ensure you have MySQL running (e.g., via XAMPP)
2. Create a database named `Social_Media_db`
3. Run the application using: `mvn spring-boot:run`
4. Access the application at http://localhost:8080
