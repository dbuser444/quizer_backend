package com.example.quizer_backend.service;

import com.example.quizer_backend.entity.Log;
import com.example.quizer_backend.entity.SystemSetting;
import com.example.quizer_backend.repository.LogRepository;
import com.example.quizer_backend.repository.SystemSettingRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemLogService {

    private final LogRepository logRepository;
    private final SystemSettingRepository settingRepository;

    /**
     * Автоматическая запись системного лога
     */
    @Transactional
    public void writeLog(Long userId, String action, String entity, Long entityId, HttpServletRequest request) {
        try {
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            Log logEntry = Log.builder()
                    .userId(userId)
                    .action(action)
                    .entity(entity)
                    .entityId(entityId)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            logRepository.save(logEntry);
            log.info("[СИСТЕМНЫЙ АУДИТ] Успешно записано действие '{}' для сущности '{}' (ID: {})", action, entity, entityId);
        } catch (Exception e) {
            log.error("Критическая ошибка при записи системного лога в БД: ", e);
        }
    }

    /**
     * Получение глобальной настройки из БД
     */
    @Transactional(readOnly = true)
    public String getSettingValue(String key, String defaultValue) {
        return settingRepository.findByKey(key)
                .map(SystemSetting::getValue)
                .orElse(defaultValue);
    }
}