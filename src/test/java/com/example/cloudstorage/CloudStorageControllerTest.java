package com.example.cloudstorage;

import com.example.cloudstorage.controller.CloudStorageController;
import com.example.cloudstorage.dto.AuthRequest;
import com.example.cloudstorage.entity.UserEntity;
import com.example.cloudstorage.service.FileStorageService;
import com.example.cloudstorage.service.TokenService;
import com.example.cloudstorage.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CloudStorageController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
public class CloudStorageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private FileStorageService fileStorageService;

    @Test
    void testLoginSuccess() throws Exception {
        // Arrange
        AuthRequest authRequest = new AuthRequest("testuser@example.com", "TestPass123!");
        UserEntity userEntity = new UserEntity();
        userEntity.setId(1L);
        userEntity.setLogin("testuser@example.com");

        when(userService.validateUser("testuser@example.com", "TestPass123!")).thenReturn(true);
        when(userService.findByLogin("testuser@example.com")).thenReturn(Optional.of(userEntity));
        when(tokenService.generateToken("testuser@example.com")).thenReturn("test-token-123");

        // Act & Assert
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authToken").value("test-token-123"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.login").value("testuser@example.com"));
    }

    @Test
    void testLoginInvalidCredentials() throws Exception {
        // Arrange
        AuthRequest authRequest = new AuthRequest("testuser@example.com", "wrongpassword");

        when(userService.validateUser("testuser@example.com", "wrongpassword")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Bad credentials"));
    }

    @Test
    void testRegisterSuccess() throws Exception {
        // Arrange
        AuthRequest authRequest = new AuthRequest("newuser@example.com", "NewPass123!");
        UserEntity newUser = new UserEntity();
        newUser.setId(2L);
        newUser.setLogin("newuser@example.com");

        when(userService.registerUser("newuser@example.com", "NewPass123!")).thenReturn(newUser);

        // Act & Assert
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully with ID: 2"));
    }

    @Test
    void testRegisterUserAlreadyExists() throws Exception {
        // Arrange
        AuthRequest authRequest = new AuthRequest("existing@example.com", "TestPass123!");

        when(userService.registerUser("existing@example.com", "TestPass123!"))
                .thenThrow(new RuntimeException("User already exists"));

        // Act & Assert
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User already exists"));
    }

    @Test
    void testListFilesWithoutToken() throws Exception {
        // Act & Assert - без токена должен возвращать 401
        mockMvc.perform(get("/list")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testListFilesWithInvalidToken() throws Exception {
        // Arrange
        when(tokenService.validateToken("invalid-token")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/list")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogoutSuccess() throws Exception {
        // Arrange
        when(tokenService.validateToken("valid-token")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/logout")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}