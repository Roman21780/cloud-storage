package com.example.cloudstorage.service;

import com.example.cloudstorage.entity.UserEntity;
import com.example.cloudstorage.repository.TokenRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TokenService {
    private final TokenRepository tokenRepository;

    public TokenService(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public String generateToken(String username) {

        String token = UUID.randomUUID().toString();

        // Сохраняем токен в базу данных
        tokenRepository.saveToken(token, username, LocalDateTime.now().plusHours(24));

        System.out.println("✅ Generated token: " + token + " for user: " + username);
        return token;
    }

    public boolean validateToken(String token) {
        boolean isValid = tokenRepository.isTokenValid(token);
        System.out.println("🔍 Validating token: " + token + ", valid: " + isValid);
        return isValid;
    }

    public String getUsernameFromToken(String token) {
        return tokenRepository.getUsernameByToken(token);
    }

    public void invalidateToken(String token) {
        tokenRepository.invalidateToken(token);
        System.out.println("🗑️ Invalidated token: " + token);
    }
}