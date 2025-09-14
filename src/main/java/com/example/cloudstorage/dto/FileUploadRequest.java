package com.example.cloudstorage.dto;

import lombok.Data;

@Data
public class FileUploadRequest {
    private String content; // base64 encoded file content
    private String contentType;
    private String originalFilename;
}
