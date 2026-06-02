package com.example.quizer_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "questions")
@Data
@NoArgsConstructor // Необходим для Hibernate
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ИСПРАВЛЕНО: Long вместо Integer

    @ManyToOne
    @JoinColumn(name = "quiz_id")
    @JsonIgnore
    private Quiz quiz;

    @Column(nullable = false) // Указываем, что текст вопроса обязателен
    private String content;

    @Column(name = "type", nullable = false)
    private String type = "single";
// По умолчанию ставим "single".
// Фронтенд будет присылать "single", "multiple" или "text".

    private Integer points = 10; // Можно поставить 10 по умолчанию

    private Integer timer = 30;

    // Важно: orphanRemoval гарантирует, что старые ответы удалятся при обновлении вопроса
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers;
}