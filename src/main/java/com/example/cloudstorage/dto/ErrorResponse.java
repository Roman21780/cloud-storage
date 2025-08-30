package com.example.cloudstorage.dto;

import lombok.Data;

@Data
public class ErrorResponse {
    private String message;
    private int id;

    public ErrorResponse(String message, int id) {
        this.message = message;
        this.id = id;
    }
}
