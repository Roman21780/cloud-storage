package com.example.cloudstorage.service;

import com.example.cloudstorage.entity.FileEntity;
import com.example.cloudstorage.entity.UserEntity;
import com.example.cloudstorage.exception.FileStorageException;
import com.example.cloudstorage.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
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

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Autowired
    private final FileRepository fileRepository;

    @Setter
    @Value("${file.storage.location}")
    private String storageLocation;


    public String storeFile(MultipartFile file, String filename, UserEntity user) {
        try {
            validateFilename(filename);

            // Сохраняем файл в файловую систему
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

            Files.write(filePath, file.getBytes());

            // Сохраняем метаданные в базу
            FileEntity fileEntity = new FileEntity();
            fileEntity.setFilename(filename);
            fileEntity.setOriginalFilename(file.getOriginalFilename());
            fileEntity.setSize(file.getSize());
            fileEntity.setContentType(file.getContentType());
            fileEntity.setUser(user);

            fileRepository.save(fileEntity);
            return fileEntity.getId().toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    public byte[] getFile(UserEntity user, String filename) throws IOException {
        log.info("Getting file: {} for user: {}", filename, user.getLogin());

        // 1. Проверить exists в базе
        Optional<FileEntity> fileEntity = fileRepository.findByUserAndFilename(user, filename);
        if (fileEntity.isEmpty()) {
            throw new FileNotFoundException("File not found in database: " + filename);
        }

        // 2. Проверить exists в файловой системе
        Path filePath = getFilePath(user, filename);
        log.info("Looking for file at: {}", filePath);

        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found in filesystem: " + filePath);
        }

        // 3. Прочитать файл
        byte[] content = Files.readAllBytes(filePath);
        log.info("File size: {} bytes", content.length);

        return content;
    }

    @Transactional
    public void deleteFile(UserEntity user, String filename) throws IOException {
        validateFilename(filename);

        // 1. Удаляем из базы данных
        int deletedCount = fileRepository.deleteByUserAndFilename(user, filename);
        if (deletedCount == 0) {
            throw new FileStorageException("File not found in database: " + filename);
        }

        // 2. Удаляем из файловой системы (если существует)
        try {
            Path filePath = getFilePath(user, filename);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            // Логируем, но не прерываем - главное что удалили из БД
            log.warn("Could not delete physical file: {}", e.getMessage());
        }
    }

    public void renameFile(UserEntity user, String oldFilename, String newFilename) throws IOException {
        log.info("Renaming {} to {} for user {}", oldFilename, newFilename, user.getLogin());

        // 1. Переименовать в файловой системе
        Path oldPath = getFilePath(user, oldFilename);
        Path newPath = getFilePath(user, newFilename);

        if (Files.exists(oldPath)) {
            Files.move(oldPath, newPath);
            log.info("File renamed in filesystem");
        }

        // 2. Переименовать в базе данных
        Optional<FileEntity> fileOpt = fileRepository.findByUserAndFilename(user, oldFilename);
        if (fileOpt.isPresent()) {
            FileEntity file = fileOpt.get();
            file.setFilename(newFilename);
            fileRepository.save(file);
            log.info("File renamed in database");
        } else {
            throw new IOException("File not found in database: " + oldFilename);
        }
    }

    public List<FileEntity> getUserFiles(UserEntity user, int limit) {
        return fileRepository.findByUserOrderByCreatedAtDesc(user);
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
