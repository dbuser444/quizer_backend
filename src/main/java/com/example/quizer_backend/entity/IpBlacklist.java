package com.example.quizer_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ip_blacklist", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ip_blacklist_id_seq")
    @SequenceGenerator(name = "ip_blacklist_id_seq", sequenceName = "ip_blacklist_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "ip_address", nullable = false, unique = true)
    private String ipAddress;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "banned_by")
    private Long bannedBy;

    @Column(name = "banned_at", nullable = false)
    private LocalDateTime bannedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        this.bannedAt = LocalDateTime.now();
    }
}