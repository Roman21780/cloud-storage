package com.example.cloudstorage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String authToken;
    private Long id;
    private String login;
    private String email;

    public AuthResponse(String authToken) {
        this.authToken = authToken;
        this.id = 0L;
        this.login = "";
        this.email = "";
    }
}