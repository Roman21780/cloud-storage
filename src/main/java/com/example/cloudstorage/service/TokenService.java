package com.example.cloudstorage.service;

import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {
    private final ConcurrentHashMap<String, String> tokenToUser = new ConcurrentHashMap<>();

    public String generateToken(String username) {
        String token = UUID.randomUUID().toString();
        tokenToUser.put(token, username);
        return token;
    }

    public boolean validateToken(String token) {
        return tokenToUser.containsKey(token);
    }

    public String getUsernameFromToken(String token) {
        return tokenToUser.get(token);
    }

    public void invalidateToken(String token) {
        tokenToUser.remove(token);
    }
}