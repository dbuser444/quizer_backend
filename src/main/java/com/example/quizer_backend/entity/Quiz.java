package com.example.quizer_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "quizzes")
@Data
@NoArgsConstructor
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ИСПРАВЛЕНО: с Integer на Long

    private String title;
    private String description;

    // Новое поле для обложки викторины
    private String imageUrl;

    @Column(name = "is_public")
    private Boolean isPublic = false;

    // cascade = ALL позволяет сохранять вопросы вместе с квизом одним запросом
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User author;
}