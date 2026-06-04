package com.example.quizer_backend.dto;

public record QuizHistory(
        String quizTitle,
        String date,
        int score,
        int totalQuestions,
        int correctAnswers
) {}