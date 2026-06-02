package com.example.quizer_backend.repository;

import com.example.quizer_backend.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    // Подсчет неудачных попыток входа с конкретного IP за последнее время
    long countByIpAddressAndSuccessAndCreatedAtAfter(String ipAddress, boolean success, LocalDateTime time);
}