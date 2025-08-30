package com.example.cloudstorage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AuthResponse {
    @JsonProperty("auth-token")
    private String authToken;

    public AuthResponse(String authToken) {
        this.authToken = authToken;
    }
}
