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


}
