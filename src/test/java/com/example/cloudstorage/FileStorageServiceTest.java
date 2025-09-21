package com.example.cloudstorage;

import com.example.cloudstorage.entity.FileEntity;
import com.example.cloudstorage.entity.UserEntity;
import com.example.cloudstorage.repository.FileRepository;
import com.example.cloudstorage.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileStorageServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private FileStorageService fileStorageService;

    private Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        // Создаем временную директорию для файлов
        tempDir = Files.createTempDirectory("file-storage-test");

        // Устанавливаем путь к файловому хранилищу через reflection
        ReflectionTestUtils.setField(fileStorageService, "storageLocation", tempDir.toString());
    }

    @Test
    void testStoreFileSuccessWithoutFileSystem() throws Exception {
        // Arrange
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setLogin("testuser");

        String filename = "test.txt";
        String originalFilename = "original_test.txt";
        long fileSize = 123L;
        String contentType = "text/plain";

        // Настройка моков
        when(multipartFile.getOriginalFilename()).thenReturn(originalFilename);
        when(multipartFile.getSize()).thenReturn(fileSize);
        when(multipartFile.getContentType()).thenReturn(contentType);
        when(multipartFile.getBytes()).thenReturn(new byte[0]); // Пустой контент

        when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> {
            FileEntity file = invocation.getArgument(0);
            file.setId(1L);
            return file;
        });

        // Используем временную директорию
        String tempDir = Files.createTempDirectory("test").toString();
        fileStorageService.setStorageLocation(tempDir);

        // Act
        String result = fileStorageService.storeFile(multipartFile, filename, user);

        // Assert
        assertNotNull(result);
        assertEquals("1", result);

        // Проверяем, что сохранили правильные данные
        verify(fileRepository).save(argThat(file ->
                file.getFilename().equals(filename) &&
                        file.getOriginalFilename().equals(originalFilename) &&
                        file.getSize() == fileSize &&
                        file.getContentType().equals(contentType) &&
                        file.getUser().equals(user)
        ));
    }

    @Test
    void testDeleteFileSuccess() throws Exception {
        // Arrange
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setLogin("testuser");

        String filename = "test.txt";

        // Создайте тестовый файл
        Path testDir = Paths.get("uploads/testuser");
        Files.createDirectories(testDir);
        Path testFile = testDir.resolve(filename);
        Files.write(testFile, "test content".getBytes());

        // Mock
        when(fileRepository.deleteByUserAndFilename(user, filename)).thenReturn(1);

        // Act
        fileStorageService.deleteFile(user, filename);

        // Assert
        verify(fileRepository, times(1)).deleteByUserAndFilename(user, filename);
    }

    @Test
    void testValidateFilenameValid() {
        // Valid filename
        assertDoesNotThrow(() -> fileStorageService.validateFilename("test.txt"));
        assertDoesNotThrow(() -> fileStorageService.validateFilename("document.pdf"));
        assertDoesNotThrow(() -> fileStorageService.validateFilename("image_123.jpg"));
    }

    @Test
    void testValidateFilenameInvalid() {
        // Invalid filenames
        assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.validateFilename("../etc/passwd"));
        assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.validateFilename("file/name.txt"));
        assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.validateFilename("file\\name.txt"));
    }

    @Test
    void testFileExists() {
        // Arrange
        UserEntity user = new UserEntity();
        user.setLogin("testuser");
        String filename = "existing.txt";

        when(fileRepository.existsByUserAndFilename(user, filename)).thenReturn(true);

        // Act & Assert
        assertTrue(fileStorageService.fileExists(user, filename));
        verify(fileRepository, times(1)).existsByUserAndFilename(user, filename);
    }

    @Test
    void testGenerateSafeFilename() {
        // Arrange
        String originalFilename = "test file.txt";

        // Act
        String safeFilename = fileStorageService.generateSafeFilename(originalFilename);

        // Assert
        assertNotNull(safeFilename);
        assertTrue(safeFilename.endsWith(".txt"));
        assertTrue(safeFilename.length() > 4); // UUID + extension
    }
}