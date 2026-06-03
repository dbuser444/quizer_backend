package com.example.quizer_backend.controller;

import com.example.quizer_backend.entity.GameSession;
import com.example.quizer_backend.entity.SessionParticipant;
import com.example.quizer_backend.repository.SessionParticipantRepository;
import com.example.quizer_backend.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/participants")
@RequiredArgsConstructor
@Slf4j
public class ParticipantRestController {

    private final QuizService quizService;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/add")
    public ResponseEntity<?> addParticipant(@RequestBody Map<String, Object> request) {
        try {
            // Безопасное чтение числа
            Number sessionIdNum = (Number) request.get("sessionId");
            if (sessionIdNum == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "sessionId отсутствует"));
            }
            Integer sessionId = sessionIdNum.intValue();
            String fullName = (String) request.get("fullName");

            log.info("[REST] Добавление участника '{}' к сессии ID: {}", fullName, sessionId);

            GameSession session = quizService.findSessionById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Сессия не найдена"));

            SessionParticipant participant = new SessionParticipant();
            participant.setSession(session);
            participant.setStudentName(fullName);
            participant.setIpAddress("REST API Connection");

            SessionParticipant savedParticipant = sessionParticipantRepository.save(participant);

            // Получаем PIN-код текущей игровой сессии для адресации в сокетах
            String pinCode = session.getPinCode();

            // Формируем структуру данных для фронтенда преподавателя
            Map<String, Object> socketPayload = Map.of(
                    "id", savedParticipant.getId(),
                    "name", savedParticipant.getStudentName()
            );

            // Отправляем оповещение в WebSocket топик
            String destination = "/topic/session/" + pinCode + "/players";
            log.info("[WEBSOCKET] Отправка уведомления о новом игроке в топик: {}", destination);

            messagingTemplate.convertAndSend(destination, (Object) socketPayload);

            return ResponseEntity.ok(Map.of("participantId", savedParticipant.getId()));
        } catch (Exception e) {
            log.error("Ошибка при добавлении участника через REST", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}