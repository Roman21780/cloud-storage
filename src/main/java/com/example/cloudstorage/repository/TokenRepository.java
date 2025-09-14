package com.example.cloudstorage.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public class TokenRepository {
    private final JdbcTemplate jdbcTemplate;

    public TokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveToken(String token, String username, LocalDateTime expiration) {
        try {
            // Получаем user_id по username
            Integer userId = jdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE login = ?",
                    Integer.class, username
            );

            jdbcTemplate.update(
                    "INSERT INTO user_tokens (user_id, token, expires_at) VALUES (?, ?, ?)",
                    userId, token, expiration
            );
            System.out.println("✅ Token saved in user_tokens table");
        } catch (Exception e) {
            System.out.println("❌ Error saving token: " + e.getMessage());
        }
    }

    public boolean isTokenValid(String token) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM user_tokens WHERE token = ? AND expires_at > NOW()",
                    Integer.class, token
            );
            return count != null && count > 0;
        } catch (Exception e) {
            System.out.println("❌ Error validating token: " + e.getMessage());
            return false;
        }
    }

    public String getUsernameByToken(String token) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT u.login FROM users u JOIN user_tokens ut ON u.id = ut.user_id WHERE ut.token = ? AND ut.expires_at > NOW()",
                    String.class, token
            );
        } catch (Exception e) {
            System.out.println("❌ Error getting username: " + e.getMessage());
            return null;
        }
    }

    public void invalidateToken(String token) {
        try {
            jdbcTemplate.update("DELETE FROM user_tokens WHERE token = ?", token);
        } catch (Exception e) {
            System.out.println("❌ Error invalidating token: " + e.getMessage());
        }
    }

    public void invalidateUserTokens(String username) {
        try {
            Integer userId = jdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE login = ?",
                    Integer.class, username
            );
            jdbcTemplate.update("DELETE FROM user_tokens WHERE user_id = ?", userId);
        } catch (Exception e) {
            System.out.println("❌ Error invalidating user tokens: " + e.getMessage());
        }
    }
}