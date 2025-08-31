package com.example.cloudstorage;

import com.example.cloudstorage.entity.UserEntity;
import com.example.cloudstorage.repository.FileRepository;
import com.example.cloudstorage.service.FileStorageService;
import com.example.cloudstorage.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileStorageServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private FileStorageService fileStorageService;

    @Mock
    private MultipartFile multipartFile;

    @Test
    void testSaveFile() throws IOException {
        UserEntity user = new UserEntity();
        user.setLogin("testuser");

        when(multipartFile.getBytes()).thenReturn(new byte[0]);
        when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
        when(multipartFile.getSize()).thenReturn(100L);
        when(multipartFile.getContentType()).thenReturn("text/plain");

        fileStorageService.saveFile(user, "test.txt", multipartFile);

        verify(fileRepository, times(1)).save(any());
    }
}
