package com.example.cloudstorage.service;

import com.example.cloudstorage.entity.FileEntity;
import com.example.cloudstorage.entity.UserEntity;
import com.example.cloudstorage.exception.FileStorageException;
import com.example.cloudstorage.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    private final FileRepository fileRepository;
    private final UserService userService;

    @Value("${file.storage.location}")
    private String storageLocation;

    public void saveFile(UserEntity user, String filename, byte[] fileContent, String contentType) throws IOException {
        validateFilename(filename);

        Path storagePath = Paths.get(storageLocation).toAbsolutePath().normalize();
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
        }

        String userDir = user.getLogin();
        Path userPath = storagePath.resolve(userDir).normalize();

        if (!userPath.startsWith(storagePath)) {
            throw new FileStorageException("Invalid file path");
        }

        if (!Files.exists(userPath)) {
            Files.createDirectories(userPath);
        }

        Path filePath = userPath.resolve(filename).normalize();

        if (!filePath.startsWith(userPath)) {
            throw new FileStorageException("Invalid file path: attempted path traversal");
        }

        Files.write(filePath, fileContent);

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFilename(filename);
        fileEntity.setOriginalFilename(filename);
        fileEntity.setSize((long) fileContent.length);
        fileEntity.setContentType(contentType);
        fileEntity.setUser(user);

        fileRepository.save(fileEntity);
    }

    public byte[] getFile(UserEntity user, String filename) throws IOException {
        validateFilename(filename);
        Path filePath = getFilePath(user, filename);
        return Files.readAllBytes(filePath);
    }

    @Transactional
    public void deleteFile(UserEntity user, String filename) throws IOException {
        validateFilename(filename);

        // Сначала удаляем файл из файловой системы
        Path filePath = getFilePath(user, filename);
        Files.deleteIfExists(filePath);

        // Затем удаляем запись из базы данных
        fileRepository.deleteByUserAndFilename(user, filename);

        System.out.println("✅ File deleted from filesystem and database: " + filename);
    }

    public void renameFile(UserEntity user, String oldFilename, String newFilename) throws IOException {
        validateFilename(oldFilename);
        validateFilename(newFilename);

        Path oldPath = getFilePath(user, oldFilename);
        Path newPath = getFilePath(user, newFilename);

        Files.move(oldPath, newPath);

        Optional<FileEntity> fileOpt = fileRepository.findByUserAndFilename(user, oldFilename);
        if (fileOpt.isPresent()) {
            FileEntity file = fileOpt.get();
            file.setFilename(newFilename);
            fileRepository.save(file);
        }
    }

    public List<FileEntity> getUserFiles(UserEntity user, int limit) {
        List<FileEntity> files = fileRepository.findByUserOrderByCreatedAtDesc(user);
        return limit > 0 ? files.stream().limit(limit).toList() : files;
    }

    private Path getFilePath(UserEntity user, String filename) throws IOException {
        validateFilename(filename);

        Path storagePath = Paths.get(storageLocation).toAbsolutePath().normalize();
        Path userPath = storagePath.resolve(user.getLogin()).normalize();
        Path filePath = userPath.resolve(filename).normalize();

        // Проверка безопасности
        if (!filePath.startsWith(userPath)) {
            throw new FileStorageException("Invalid file path: attempted path traversal");
        }

        return filePath;
    }

    public boolean fileExists(UserEntity user, String filename) {
        try {
            validateFilename(filename);
            return fileRepository.existsByUserAndFilename(user, filename);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // Валидация имени файла против Path Traversal
    public void validateFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        // Проверка на попытку Path Traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename: path traversal attempt detected");
        }

        // Проверка на недопустимые символы
        if (!filename.matches("^[a-zA-Z0-9._-]+$")) {
            throw new IllegalArgumentException("Filename contains invalid characters");
        }

        // Проверка длины
        if (filename.length() > 255) {
            throw new IllegalArgumentException("Filename too long");
        }
    }

    // Альтернативный безопасный подход: генерация уникального имени
    public String generateSafeFilename(String originalFilename) {
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }

        // Генерация UUID + оригинальное расширение
        return UUID.randomUUID() + extension;
    }
}
