package com.example.quizer_backend.dto;

public record PlayerResult(
        String studentName,
        int score,
        float responseTime
) {}