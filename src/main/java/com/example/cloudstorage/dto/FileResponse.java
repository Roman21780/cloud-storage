package com.example.cloudstorage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileResponse {
    @JsonProperty("filename")
    private String filename;

    @JsonProperty("size")
    private Long size;

    public FileResponse(String filename, Long size, LocalDateTime createdAt) {
        this.filename = filename;
        this.size = size;
    }
}
