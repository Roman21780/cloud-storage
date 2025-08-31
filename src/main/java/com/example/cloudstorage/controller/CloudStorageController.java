package com.example.cloudstorage.controller;

import com.example.cloudstorage.dto.*;
import com.example.cloudstorage.entity.FileEntity;
import com.example.cloudstorage.entity.UserEntity;
import com.example.cloudstorage.service.FileStorageService;
import com.example.cloudstorage.service.TokenService;
import com.example.cloudstorage.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class CloudStorageController {
    private final UserService userService;
    private final TokenService tokenService;
    private final FileStorageService fileStorageService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authRequest) {
        if (userService.validateUser(authRequest.getLogin(), authRequest.getPassword())) {
            String token = tokenService.generateToken(authRequest.getLogin());
            return ResponseEntity.ok(new AuthResponse(token));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Bad credentials", 400));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("auth-token") String token) {
        if (tokenService.validateToken(token)) {
            tokenService.invalidateToken(token);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Bad credentials", 401));
    }
}
