package com.example.quizer_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_sessions")
@Data
@NoArgsConstructor // Необходим для Hibernate
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "pin_code", nullable = false, length = 10)
    private String pinCode;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "active"; // "waiting", "active", "finished"

    // === ДОБАВЛЕННЫЕ ПОЛЯ ДЛЯ ФИКСАЦИИ ВРЕМЕНИ И ИСПРАВЛЕНИЯ ОШИБОК ===

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;
}