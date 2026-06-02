package com.example.quizer_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "security_events", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "security_events_id_seq")
    @SequenceGenerator(name = "security_events_id_seq", sequenceName = "security_events_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "email")
    private String email;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "resolved", nullable = false)
    private boolean resolved;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.resolved = false;
    }
}