package com.itransform.authservice.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id
    private String id;
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password; // BCrypt hash
    
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "ADMIN|MANAGER|SUPPORT|USER", message = "Role must be one of: ADMIN, MANAGER, SUPPORT, USER")
    private String role; // ADMIN, MANAGER, SUPPORT, USER
}
