package com.example.cloudstorage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FileResponse {
    @JsonProperty("filename")
    private String filename;

    @JsonProperty("size")
    private Long size;

    public FileResponse(String filename, Long size) {
        this.filename = filename;
        this.size = size;
    }
}
