package com.example.cloudstorage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Login cannot be blank")
    @Size(min = 3, max = 50, message = "Login must be between 3 and 50 characters")
    private String login;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}