package com.example.quizer_backend.service;

import com.example.quizer_backend.entity.IpBlacklist;
import com.example.quizer_backend.entity.LoginAttempt;
import com.example.quizer_backend.entity.SecurityEvent;
import com.example.quizer_backend.repository.IpBlacklistRepository;
import com.example.quizer_backend.repository.LoginAttemptRepository;
import com.example.quizer_backend.repository.SecurityEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityAnomaliesService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final SecurityEventRepository securityEventRepository;
    private final IpBlacklistRepository ipBlacklistRepository;

    private static final int MAX_ATTEMPTS = 5;
    private static final int BAN_DURATION_HOURS = 24;

    @Transactional
    public void registerLoginAttempt(String email, boolean success, String ipAddress) {
        // 1. Запись в таблицу login_attempts
        LoginAttempt attempt = LoginAttempt.builder()
                .email(email)
                .success(success)
                .ipAddress(ipAddress)
                .build();
        loginAttemptRepository.save(attempt);

        if (!success) {
            // 2. Проверка лимита ошибок за последние 10 минут
            LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
            long failedCount = loginAttemptRepository.countByIpAddressAndSuccessAndCreatedAtAfter(ipAddress, false, tenMinutesAgo);

            log.warn("[ОХРАНА] Неудачная попытка входа для {}. Всего ошибок с IP {}: {}", email, ipAddress, failedCount);

            if (failedCount >= MAX_ATTEMPTS) {
                triggerIpBlock(ipAddress, email);
            }
        } else {
            log.info("[ОХРАНА] Успешная авторизация пользователя: {} с IP: {}", email, ipAddress);
        }
    }

    private void triggerIpBlock(String ipAddress, String email) {
        // Проверяем, нет ли уже активного бана
        if (ipBlacklistRepository.existsByIpAddressAndExpiresAtAfter(ipAddress, LocalDateTime.now())) {
            return;
        }

        log.error("[КРИТИЧЕСКИЙ АУДИТ] Превышен лимит попыток входа! Блокировка IP: {}", ipAddress);

        // 3. Запись инцидента безопасности в security_events
        SecurityEvent event = SecurityEvent.builder()
                .ipAddress(ipAddress)
                .email(email)
                .eventType("BRUTEFORCE_ATTEMPT")
                .details("Автоматическая блокировка: более " + MAX_ATTEMPTS + " неудачных попыток входа за 10 минут.")
                .build();
        securityEventRepository.save(event);

        // 4. Добавление IP в ip_blacklist на сутки
        IpBlacklist ban = IpBlacklist.builder()
                .ipAddress(ipAddress)
                .reason("Подозрение на перебор паролей (Bruteforce)")
                .expiresAt(LocalDateTime.now().plusHours(BAN_DURATION_HOURS))
                .build();
        ipBlacklistRepository.save(ban);
    }
}