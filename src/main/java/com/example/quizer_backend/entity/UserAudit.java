package com.example.quizer_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_audit", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_audit_id_seq")
    @SequenceGenerator(name = "user_audit_id_seq", sequenceName = "user_audit_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "field_changed", nullable = false)
    private String fieldChanged;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}