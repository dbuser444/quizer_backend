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

    @Column(name = "quiz_id") // ID самой викторины (теста)
    private Integer quizId;

    @Column(name = "question_id") // <-- ДОБАВЛЕНО ПОЛЕ ДЛЯ ИДЕНТИФИКАТОРА ВОПРОСА
    private Integer questionId;

    @Column(name = "answer_id") // ID выбранного ответа
    private Integer answerId;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "time_spent")
    private Double timeSpent;
}