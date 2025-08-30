package com.example.cloudstorage.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RenameRequest {
    @NotBlank(message = "New name is required")
    private String name;
}
