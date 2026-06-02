package com.example.quizer_backend.service;

import com.example.quizer_backend.entity.WebSocketLog;
import com.example.quizer_backend.repository.WebSocketLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketAudioLogService {

    private final WebSocketLogRepository webSocketLogRepository;

    /**
     * Асинхронное сохранение логов интерактивного взаимодействия в базу данных
     */
    @Async
    @Transactional
    public void logWebSocketEvent(Integer sessionId, Long participantId, String eventType, String payload, String ipAddress) {
        try {
            // Обрезаем payload, если он слишком огромный, чтобы не перегружать СУБД
            String optimizedPayload = (payload != null && payload.length() > 1000)
                    ? payload.substring(0, 1000) + "... [TRUNCATED]"
                    : payload;

            WebSocketLog socketLog = WebSocketLog.builder()
                    .sessionId(sessionId)
                    .participantId(participantId)
                    .eventType(eventType)
                    .payload(optimizedPayload)
                    .clientIp(ipAddress)
                    .build();

            webSocketLogRepository.save(socketLog);
            log.info("[WEBSOCKET ТРАФИК] Зафиксировано событие '{}' для сессии ID: {}", eventType, sessionId);
        } catch (Exception e) {
            log.error("Не удалось сохранить лог WebSocket-сообщения в БД: ", e);
        }
    }
}