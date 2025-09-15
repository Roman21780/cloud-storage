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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileStorageServiceTest {

    @Mock
    private FileRepository fileRepository;

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
    void testSaveFileSuccess() throws Exception {
        // Arrange
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setLogin("testuser");

        byte[] fileContent = "test content".getBytes();
        String filename = "test.txt";
        String contentType = "text/plain";

        when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> {
            FileEntity file = invocation.getArgument(0);
            file.setId(1L);
            return file;
        });

        // Act
        fileStorageService.saveFile(user, filename, fileContent, contentType);

        // Assert
        verify(fileRepository, times(1)).save(any(FileEntity.class));
        // Проверяем, что файл был создан
        assertTrue(Files.exists(tempDir.resolve("testuser").resolve("test.txt")));
    }

    @Test
    void testDeleteFileSuccess() throws Exception {
        // Arrange
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setLogin("testuser");
        String filename = "test.txt";

        // Сначала создаем файл для удаления
        Path userDir = tempDir.resolve("testuser");
        Files.createDirectories(userDir);
        Files.write(userDir.resolve("test.txt"), "test content".getBytes());

        // Убираем ненужный стаббинг, так как метод deleteByUserAndFilename не требует мокирования
        // когда(fileRepository.findByUserAndFilename(user, filename)).thenReturn(Optional.of(fileEntity));

        // Act
        fileStorageService.deleteFile(user, filename);

        // Assert
        verify(fileRepository, times(1)).deleteByUserAndFilename(user, filename);
        // Проверяем, что файл был удален
        assertFalse(Files.exists(userDir.resolve("test.txt")));
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