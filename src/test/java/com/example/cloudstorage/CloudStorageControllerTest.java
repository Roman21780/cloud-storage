package com.example.cloudstorage;

import com.example.cloudstorage.controller.CloudStorageController;
import com.example.cloudstorage.service.FileStorageService;
import com.example.cloudstorage.service.TokenService;
import com.example.cloudstorage.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CloudStorageController.class)
public class CloudStorageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private FileStorageService fileStorageService;

    @Test
    void testLoginSuccess() throws Exception {
        when(userService.validateUser("testuser", "password123")).thenReturn(true);
        when(tokenService.generateToken("testuser")).thenReturn("test-token");

        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"testuser\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auth-token").value("test-token"));

    }
}
