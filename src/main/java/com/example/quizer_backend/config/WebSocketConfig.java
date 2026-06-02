package com.example.quizer_backend.config;

import com.example.quizer_backend.entity.GameSession;
import com.example.quizer_backend.repository.SessionRepository;
import com.example.quizer_backend.service.WebSocketAudioLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Optional;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAudioLogService webSocketAudioLogService;
    private final SessionRepository sessionRepository; // ДОБАВЛЕНО: Репозиторий для поиска реального ID сессии по PIN

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Каналы для рассылки (сервер -> клиент)
        config.enableSimpleBroker("/topic", "/queue");

        // Префикс для эндпоинтов в контроллерах (клиент -> сервер)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Поддержка CORS и SockJS подключений
        registry.addEndpoint("/ws-quiz")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Регистрируем кастомный перехватчик для логирования входящего сокет-трафика
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                StompCommand command = accessor.getCommand();

                if (command != null) {
                    String eventType = command.name();
                    String destination = accessor.getDestination();

                    // Отбираем только ключевые события, игнорируя системные пинги (сердцебиение)
                    if (command == StompCommand.CONNECT || command == StompCommand.SEND || command == StompCommand.DISCONNECT) {

                        // ИСПРАВЛЕНО: Заворачиваем весь блок логирования в try-catch!
                        // Если логирование упадет, мы поймаем ошибку, но НЕ уроним сокет-соединение.
                        try {
                            Integer realSessionId = null;

                            if (destination != null) {
                                // Вытаскиваем ПИН-код из пути (например, /app/quiz/177760/join или /topic/session/177760/players)
                                String pinCode = extractPinFromDestination(destination);

                                if (pinCode != null) {
                                    // Ищем сессию в БД по ПИН-коду, чтобы узнать её настоящий числовой ID
                                    Optional<GameSession> actualSession = sessionRepository.findByPinCode(pinCode);
                                    if (actualSession.isPresent()) {
                                        realSessionId = actualSession.get().getId();
                                    }
                                }
                            }

                            // Извлекаем тело сообщения в виде строки
                            String payload = null;
                            if (message.getPayload() instanceof byte[]) {
                                payload = new String((byte[]) message.getPayload());
                            }

                            // Формируем красивое описание для лога
                            String formattedPayload = destination != null
                                    ? "Назначение: " + destination + " | Тело: " + payload
                                    : payload;

                            // Вызываем твой сервис для записи в таблицу websocket_logs
                            // Передаем realSessionId (который мы нашли по ПИН-коду, либо null, если это общий коннект)
                            webSocketAudioLogService.logWebSocketEvent(
                                    realSessionId,
                                    null,
                                    eventType,
                                    formattedPayload,
                                    "WebSocket Connection"
                            );

                        } catch (Exception e) {
                            // КРИТИЧЕСКИЙ КЛЮЧ К СТАБИЛЬНОСТИ:
                            // Логируем ошибку записи лога в консоль бэкенда, но не пробрасываем её дальше!
                            log.error("[ОШИБКА АУДИТА СОКЕТОВ] Не удалось сохранить запись в websocket_logs, но игра продолжается: ", e);
                        }
                    }
                }
                return message;
            }
        });
    }

    /**
     * Вспомогательный метод для безопасного извлечения ПИН-кода из сокет-путей
     */
    private String extractPinFromDestination(String destination) {
        try {
            String[] segments = destination.split("/");
            for (int i = 0; i < segments.length; i++) {
                // Если сегмент равен "quiz" или "session", то следующий за ним сегмент — это ПИН-код
                if (("quiz".equals(segments[i]) || "session".equals(segments[i])) && (i + 1 < segments.length)) {
                    String possiblePin = segments[i + 1];
                    // Проверяем регуляркой, что сегмент состоит полностью из цифр
                    if (possiblePin.matches("\\d+")) {
                        return possiblePin;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}