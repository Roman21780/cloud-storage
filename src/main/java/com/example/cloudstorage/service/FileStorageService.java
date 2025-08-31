package com.example.cloudstorage.service;

import com.example.cloudstorage.entity.FileEntity;
import com.example.cloudstorage.entity.UserEntity;
import com.example.cloudstorage.repository.FileRepository;
import com.example.cloudstorage.util.FileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    private final FileRepository fileRepository;
    private final UserService userService;

    @Value("${file.storage.location}")
    private String storageLocation;

    public void saveFile(UserEntity user, String filename, MultipartFile file) throws IOException {
        Path storagePath = Paths.get(storageLocation);
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
        }

        String userDir = user.getLogin();
        Path userPath = storagePath.resolve(userDir);
        if (!Files.exists(userPath)) {
            Files.createDirectories(userPath);
        }

        Path filePath = userPath.resolve(filename);
        Files.write(filePath, file.getBytes());

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFilename(filename);
        fileEntity.setOriginalFilename(file.getOriginalFilename());
        fileEntity.setSize(file.getSize());
        fileEntity.setContentType(file.getContentType());
        fileEntity.setUser(user);

        fileRepository.save(fileEntity);
    }

    public byte[] getFile(UserEntity user, String filename) throws  IOException {
        Path filePath = getFilePath(user, filename);
        return Files.readAllBytes(filePath);
    }

    public void deleteFile(UserEntity user, String filename) throws IOException {
        Path filePath = getFilePath(user, filename);
        Files.deleteIfExists(filePath);
        fileRepository.deleteByUserAndFilename(user, filename);
    }

    public void renameFile(UserEntity user, String oldFilename, String newFilename) throws IOException {
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

    private Path getFilePath(UserEntity user, String filename) {
        return Paths.get(storageLocation).resolve(user.getLogin()).resolve(filename);
    }

    public boolean fileExists(UserEntity user, String filename) {
        return fileRepository.existsByUserAndFilename(user, filename);
    }
}
