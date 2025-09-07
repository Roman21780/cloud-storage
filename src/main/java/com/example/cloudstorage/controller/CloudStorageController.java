package com.example.cloudstorage.controller;

import com.example.cloudstorage.dto.*;
import com.example.cloudstorage.entity.FileEntity;
import com.example.cloudstorage.entity.UserEntity;
import com.example.cloudstorage.exception.FileStorageException;
import com.example.cloudstorage.service.FileStorageService;
import com.example.cloudstorage.service.TokenService;
import com.example.cloudstorage.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
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


    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            UserEntity registeredUser = userService.registerUser(registerRequest.getLogin(), registerRequest.getPassword());
            return ResponseEntity.ok(new RegisterResponse(true, "User registered successfully with ID: " + registeredUser.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new RegisterResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authRequest) {
        if (userService.validateUser(authRequest.getLogin(), authRequest.getPassword())) {
            String token = tokenService.generateToken(authRequest.getLogin());

            UserEntity user = userService.findByLogin(authRequest.getLogin())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return ResponseEntity.ok(new AuthResponse(
                    token,
                    user.getId(),
                    user.getLogin(),
                    user.getLogin() // Используем login как email
            ));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Bad credentials", 401));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("auth-token") String token) {
        if (tokenService.validateToken(token)) {
            tokenService.invalidateToken(token);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Unauthorized", 401));
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") @Pattern(regexp = "^[a-zA-Z0-9._-]+$") String filename,
            @RequestPart("file") MultipartFile file) {

        if (!tokenService.validateToken(token)) {
            return unauthorizedResponse();
        }

        String username = tokenService.getUsernameFromToken(token);
        UserEntity user = userService.findByLogin(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            if (fileStorageService.fileExists(user, filename)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("File already exists", 400));
            }

            fileStorageService.saveFile(user, filename, file);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error uploading file", 500));
        }
    }

    @GetMapping("/file")
    public ResponseEntity<?> downloadFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename) {

        if (!tokenService.validateToken(token)) {
            return unauthorizedResponse();
        }

        String username = tokenService.getUsernameFromToken(token);
        UserEntity user = userService.findByLogin(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            byte[] fileContent = fileStorageService.getFile(user, filename);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(fileContent);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error downloading file", 500));
        }
    }

    @DeleteMapping("/file")
    public ResponseEntity<?> deleteFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename) {

        if (!tokenService.validateToken(token)) {
            return unauthorizedResponse();
        }

        String username = tokenService.getUsernameFromToken(token);
        UserEntity user = userService.findByLogin(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            fileStorageService.deleteFile(user, filename);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error deleting file", 500));
        }
    }

    @PutMapping("/file")
    public ResponseEntity<?> renameFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename,
            @Valid @RequestBody RenameRequest renameRequest) {

        if (!tokenService.validateToken(token)) {
            return unauthorizedResponse();
        }

        String username = tokenService.getUsernameFromToken(token);
        UserEntity user = userService.findByLogin(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            fileStorageService.renameFile(user, filename, renameRequest.getName());
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error renaming file", 500));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> listFiles(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "auth-token", required = false) String authToken,
            @RequestParam(value = "limit", defaultValue = "0") int limit) {

        System.out.println("Authorization header: " + authHeader);
        System.out.println("Auth token header: " + authToken);

        // Проверяем оба варианта передачи токена
        String token = authToken;
        if (token == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        System.out.println("Final token: " + token);

        if (token == null) {
            System.out.println("Token is null");
            return unauthorizedResponse();
        }

        if (!tokenService.validateToken(token)) {
            System.out.println("Token validation failed");
            return unauthorizedResponse();
        }

        System.out.println("Token validated successfully");

        String username = tokenService.getUsernameFromToken(token);
        UserEntity user = userService.findByLogin(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<FileEntity> files = fileStorageService.getUserFiles(user, limit);
        List<FileResponse> response = files.stream()
                .map(file -> new FileResponse(file.getFilename(), file.getSize()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage(), 400));
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorageException(FileStorageException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage(), 400));
    }

    private ResponseEntity<ErrorResponse> unauthorizedResponse() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Unauthorized", 401));
    }
}
