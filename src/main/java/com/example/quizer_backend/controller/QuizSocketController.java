package com.example.quizer_backend.controller;

import com.example.quizer_backend.entity.GameSession;
import com.example.quizer_backend.entity.SessionParticipant;
import com.example.quizer_backend.entity.QuizResult;
import com.example.quizer_backend.repository.SessionParticipantRepository;
import com.example.quizer_backend.repository.SessionRepository;
import com.example.quizer_backend.repository.QuizResultRepository;
import com.example.quizer_backend.repository.UserRepository;
import com.example.quizer_backend.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class QuizSocketController {

    private final QuizService quizService;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final QuizResultRepository quizResultRepository;

    /**
     * Регистрация участника по PIN-коду с привязкой к его учетной записи USER_ID.
     */
    @MessageMapping("/quiz/{pin}/join")
    @SendTo("/topic/session/{pin}/players")
    public List<String> joinPlayer(@DestinationVariable String pin, Map<String, String> player) {
        String nickname = player.get("name");
        String studentUsername = player.get("username");

        log.info("[WEBSOCKET] Регистрация участника '{}' (Аккаунт: {}) по PIN-коду: {}", nickname, studentUsername, pin);

        GameSession session = quizService.findActiveSessionByPin(pin)
                .orElseThrow(() -> new NoSuchElementException("Активная сессия не найдена"));

        SessionParticipant participant = new SessionParticipant();
        participant.setSession(session);
        participant.setStudentName(nickname);
        participant.setIpAddress("WebSocket Connection");

        if (studentUsername != null && !studentUsername.isBlank()) {
            userRepository.findByUsername(studentUsername).ifPresent(user -> {
                try {
                    participant.getClass().getMethod("setUser", user.getClass()).invoke(participant, user);
                    log.info("Участник '{}' успешно привязан к аккаунту: {}", nickname, studentUsername);
                } catch (Exception e) {
                    try {
                        participant.getClass().getMethod("setStudent", user.getClass()).invoke(participant, user);
                        log.info("Участник '{}' успешно привязан к аккаунту через setStudent: {}", nickname, studentUsername);
                    } catch (Exception ex) {
                        log.warn("Не удалось связать SessionParticipant с User. Проверь наличие поля User в SessionParticipant.");
                    }
                }
            });
        } else {
            log.warn("Внимание: Мобильное приложение не передало 'username' студента. user_id в сессии останется null!");
        }

        sessionParticipantRepository.save(participant);

        List<SessionParticipant> allParticipants = sessionParticipantRepository.findBySessionId(session.getId());
        return allParticipants.stream()
                .map(SessionParticipant::getStudentName)
                .collect(Collectors.toList());
    }

    /**
     * ОБНОВЛЕНО ДЛЯ САМОСТОЯТЕЛЬНОГО ПРОХОЖДЕНИЯ СТУДЕНТАМИ.
     */
    @MessageMapping("/quiz/{pin}/start")
    @SendTo("/topic/session/{pin}/game-flow")
    @Transactional
    public Map<String, Object> startQuiz(@DestinationVariable String pin) {
        log.info("[WEBSOCKET] Асинхронный запуск викторины для сессии PIN: {}", pin);

        GameSession session = quizService.findActiveSessionByPin(pin)
                .orElseThrow(() -> new NoSuchElementException("Активная сессия не найдена"));

        var questions = session.getQuiz().getQuestions();

        if (questions == null || questions.isEmpty()) {
            log.error("Ошибка сокета: в викторине '{}' отсутствуют вопросы!", session.getQuiz().getTitle());
            return Map.of("action", "ERROR", "message", "В викторине нет вопросов");
        }

        questions.forEach(question -> {
            if (question.getAnswers() != null) {
                question.getAnswers().size();
            }
        });

        log.info("Отправляем на mobile устройства весь массив вопросов с ответами. Всего заданий: {} шт.", questions.size());

        return Map.of(
                "action", "START_SESSION",
                "questions", questions
        );
    }

    /**
     * ОБНОВЛЕНО: Прием ответов от мобильного приложения по веб-сокетам.
     * Теперь данные раскладываются четко по полям quizId (ID викторины) и questionId (ID вопроса).
     */
    @MessageMapping("/quiz/session/{sessionId}/submit-answer")
    @SendTo("/topic/session/{sessionId}/progress")
    @Transactional
    public Map<String, Object> receiveStudentAnswer(@DestinationVariable Integer sessionId, Map<String, Object> payload) {
        log.info("[WEBSOCKET] Получен ответ от студента для сессии ID {}: {}", sessionId, payload);

        String studentName = (String) payload.get("studentName");

        // Безопасно извлекаем пришедший ID вопроса
        Integer targetQuestionId = ((Number) payload.get("questionId")).intValue();
        Integer answerId = payload.get("answerId") != null ? ((Number) payload.get("answerId")).intValue() : null;
        Double timeSpent = payload.get("timeSpent") != null ? ((Number) payload.get("timeSpent")).doubleValue() : 0.0;

        GameSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Сессия с ID " + sessionId + " не найдена"));

        // Ищем вопрос в структуре теста
        var question = session.getQuiz().getQuestions().stream()
                .filter(q -> q.getId().longValue() == targetQuestionId.longValue())
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Вопрос с ID " + targetQuestionId + " не найден в этой викторине"));

        // Проверяем правильность
        boolean isCorrect = false;
        if (answerId != null && question.getAnswers() != null) {
            isCorrect = question.getAnswers().stream()
                    .filter(a -> a.getId().longValue() == answerId.longValue())
                    .map(com.example.quizer_backend.entity.Answer::getIsCorrect)
                    .findFirst()
                    .orElse(false);
        }

        // ОБНОВЛЕНО: Мапим данные в базу строго по новой структуре QuizResult
        QuizResult result = new QuizResult();
        result.setSessionId(sessionId);
        result.setStudentName(studentName != null ? studentName : "Аноним");
        result.setQuizId(session.getQuiz().getId().intValue()); // ID самого теста (викторины)
        result.setQuestionId(targetQuestionId);                 // ID конкретного текущего вопроса
        result.setAnswerId(answerId);
        result.setIsCorrect(isCorrect);
        result.setTimeSpent(timeSpent);

        if (payload.get("userId") != null) {
            result.setUserId(((Number) payload.get("userId")).intValue());
        }

        quizResultRepository.save(result);

        log.info("Ответ сохранен в таблицу 'results'. Студент: {}, Тест ID: {}, Вопрос ID: {}, Верно: {}",
                result.getStudentName(), result.getQuizId(), targetQuestionId, isCorrect);

        return Map.of(
                "action", "STUDENT_PROGRESS",
                "studentName", result.getStudentName(),
                "quizId", targetQuestionId, // Передаем ID вопроса на фронтенд панели
                "isCorrect", isCorrect
        );
    }

    /**
     * Команда изменения шага организатором.
     */
    @MessageMapping("/quiz/{pin}/next-question")
    @SendTo("/topic/session/{pin}/game-flow")
    public Map<String, Object> nextQuestion(@DestinationVariable String pin, Map<String, Object> payload) {
        return Map.of("action", "FREE_PACE_ACTIVE");
    }

    /**
     * Команда показа результатов текущего вопроса.
     */
    @MessageMapping("/quiz/{pin}/show-results")
    @SendTo("/topic/session/{pin}/game-flow")
    public Map<String, Object> showResults(@DestinationVariable String pin, Map<String, Object> payload) {
        return Map.of("action", "MONITORING_REFRESH");
    }

    /**
     * Принудительная отмена викторины преподавателем.
     */
    @MessageMapping("/quiz/{pin}/cancel")
    @SendTo("/topic/session/{pin}/game-flow")
    @Transactional
    public Map<String, Object> cancelQuiz(@DestinationVariable String pin, Map<String, Object> payload) {
        quizService.findActiveSessionByPin(pin).ifPresent(session -> {
            session.setStatus("finished");
            session.setFinishedAt(LocalDateTime.now());
            sessionRepository.save(session);
        });

        payload.put("action", "TERMINATE_GAME_SESSION");
        return payload;
    }

    /**
     * Успешное завершение викторины преподавателем или по таймеру с лидербордом на основе QuizResult.
     */
    @MessageMapping("/quiz/{pin}/end")
    @SendTo("/topic/session/{pin}/game-flow")
    @Transactional
    public Map<String, Object> endQuiz(@DestinationVariable String pin, Map<String, Object> payload) {
        log.info("[WEBSOCKET] Сигнал закрытия викторины для сессии с PIN {}", pin);

        GameSession session = quizService.findActiveSessionByPin(pin)
                .orElseThrow(() -> new NoSuchElementException("Сессия не найдена"));

        session.setStatus("finished");
        session.setFinishedAt(LocalDateTime.now());
        sessionRepository.save(session);

        // Извлекаем ответы из таблицы 'results'
        List<QuizResult> allResults = quizResultRepository.findBySessionId(session.getId());

        // Группируем по имени студента и считаем сумму флагов isCorrect == true
        Map<String, Long> scoreMap = allResults.stream()
                .filter(r -> r.getIsCorrect() != null && r.getIsCorrect())
                .collect(Collectors.groupingBy(
                        QuizResult::getStudentName,
                        Collectors.counting()
                ));

        List<SessionParticipant> participants = sessionParticipantRepository.findBySessionId(session.getId());

        List<Map<String, Object>> leaderboard = participants.stream()
                .map(p -> {
                    String name = p.getStudentName() != null ? p.getStudentName() : "Аноним";
                    long finalScore = scoreMap.getOrDefault(name, 0L);
                    return Map.<String, Object>of(
                            "studentName", name,
                            "score", finalScore
                    );
                })
                .sorted((a, b) -> Long.compare((Long) b.get("score"), (Long) a.get("score")))
                .collect(Collectors.toList());

        return Map.of(
                "action", "FINISH_GAME_SESSION",
                "sessionId", session.getId(),
                "results", leaderboard
        );
    }
}