package com.example.cloudstorage.controller;

import com.example.cloudstorage.dto.*;
import com.example.cloudstorage.entity.FileEntity;
import com.example.cloudstorage.entity.UserEntity;
import com.example.cloudstorage.exception.FileStorageException;
import com.example.cloudstorage.service.FileStorageService;
import com.example.cloudstorage.service.TokenService;
import com.example.cloudstorage.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
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
                    user.getLogin()
            ));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Bad credentials", 401));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "auth-token", required = false) String authToken) {

        String token = extractTokenFromHeaders(authHeader, authToken);
        logRequest("Logout", authHeader, authToken, token);

        if (token == null) {
            System.out.println("❌ No token provided for logout");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("No token provided", 400));
        }

        if (!tokenService.validateToken(token)) {
            System.out.println("❌ Invalid token for logout: " + token);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token", 401));
        }

        try {
            tokenService.invalidateToken(token);
            System.out.println("✅ Logout successful for token: " + token);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.out.println("❌ Error during logout: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Logout failed", 500));
        }
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("filename") String filename,
            @RequestParam("file") MultipartFile file) {

        try {
            // Валидация токена
            String token = authHeader.replace("Bearer ", "");
            if (!tokenService.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }

            String username = tokenService.getUsernameFromToken(token);
            UserEntity user = userService.findByLogin(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Сохранение файла
            String fileId = fileStorageService.storeFile(file, filename, user);

            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "message", "File uploaded successfully",
                    "fileId", fileId
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "File upload failed: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/file")
    public ResponseEntity<?> downloadFile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "auth-token", required = false) String authToken,
            @RequestParam("filename") String filename) {

        String token = extractTokenFromHeaders(authHeader, authToken);
        logRequest("File download", authHeader, authToken, token);

        if (token == null || !tokenService.validateToken(token)) {
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
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "auth-token", required = false) String authToken,
            @RequestParam("filename") String filename) {

        String token = extractTokenFromHeaders(authHeader, authToken);
        logRequest("File delete", authHeader, authToken, token);

        if (token == null || !tokenService.validateToken(token)) {
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
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "auth-token", required = false) String authToken,
            @RequestParam("filename") String filename,
            @Valid @RequestBody RenameRequest renameRequest) {

        String token = extractTokenFromHeaders(authHeader, authToken);
        logRequest("File rename", authHeader, authToken, token);

        if (token == null || !tokenService.validateToken(token)) {
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
            HttpServletRequest request, // Добавьте это
            @RequestParam(value = "limit", defaultValue = "0") int limit) {

        System.out.println("=== LIST FILES REQUEST ===");

        // Логируем все заголовки
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            System.out.println(headerName + ": " + request.getHeader(headerName));
        }

        String token = extractTokenFromHeaders(authHeader, authToken);
        System.out.println("Extracted token: " + token);

        if (token == null) {
            System.out.println("❌ Token is null - returning 403");
            return unauthorizedResponse();
        }

        System.out.println("🔍 Validating token: " + token);
        boolean isValid = tokenService.validateToken(token);
        System.out.println("Token validation result: " + isValid);

        if (!isValid) {
            System.out.println("❌ Token validation failed - returning 403");
            return unauthorizedResponse();
        }

        System.out.println("✅ Token validated successfully");

        String username = tokenService.getUsernameFromToken(token);
        System.out.println("Retrieved username: " + username);

        if (username == null) {
            System.out.println("❌ Username is null - returning 403");
            return unauthorizedResponse();
        }

        try {
            UserEntity user = userService.findByLogin(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            List<FileEntity> files = fileStorageService.getUserFiles(user, limit);
            List<FileResponse> response = files.stream()
                    .map(file -> new FileResponse(file.getFilename(), file.getSize()))
                    .collect(Collectors.toList());

            System.out.println("✅ Successfully returned " + files.size() + " files");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("❌ Error in listFiles: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal server error", 500));
        }
    }

    // Вспомогательные методы
    private String extractTokenFromHeaders(String authHeader, String authToken) {
        String token = authToken;
        if (token == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        return token;
    }

    private void logRequest(String operation, String authHeader, String authToken, String extractedToken) {
        System.out.println("=== " + operation + " ===");
        System.out.println("Authorization header: " + authHeader);
        System.out.println("Auth token header: " + authToken);
        System.out.println("Extracted token: " + extractedToken);
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

    private void logAllHeaders(HttpServletRequest request) {
        System.out.println("=== REQUEST HEADERS ===");
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            System.out.println(headerName + ": " + request.getHeader(headerName));
        }
        System.out.println("=== END HEADERS ===");
    }
}