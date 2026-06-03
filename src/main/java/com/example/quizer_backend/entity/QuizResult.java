package com.example.quizer_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "results")
@Data
public class QuizResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "session_id")
    private Integer sessionId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "student_name")
    private String studentName;

    @Column(name = "quiz_id")
    private Integer quizId;

    @Column(name = "question_id")
    private Integer questionId;

    @Column(name = "answer_id")
    private Integer answerId;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "response_time")
    private Double responseTime;

    @Column(name = "time_spent")
    private Double timeSpent;
}