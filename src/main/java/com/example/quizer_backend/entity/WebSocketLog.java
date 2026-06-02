package com.example.quizer_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "websocket_logs", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "websocket_logs_id_seq")
    @SequenceGenerator(name = "websocket_logs_id_seq", sequenceName = "websocket_logs_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "session_id")
    private Integer sessionId; // Привязано к ID игровой сессии (Integer согласно архитектуре проекта)

    @Column(name = "participant_id")
    private Long participantId; // Привязано к ID участника-студента

    @Column(name = "event_type", nullable = false)
    private String eventType; // CONNECT, DISCONNECT, SEND_ANSWER, NEXT_QUESTION

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload; // Тело сообщения (JSON строка или системный фрейм)

    @Column(name = "client_ip")
    private String clientIp;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}