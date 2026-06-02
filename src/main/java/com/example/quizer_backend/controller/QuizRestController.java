package com.example.quizer_backend.controller;

import com.example.quizer_backend.entity.GameSession;
import com.example.quizer_backend.entity.Quiz;
import com.example.quizer_backend.entity.User;
import com.example.quizer_backend.entity.QuizResult;
import com.example.quizer_backend.repository.QuizRepository;
import com.example.quizer_backend.repository.UserRepository;
import com.example.quizer_backend.repository.SessionRepository;
import com.example.quizer_backend.repository.QuizResultRepository;
import com.example.quizer_backend.service.QuizService;
import com.example.quizer_backend.service.SystemLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quizzes")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
@Slf4j
public class QuizRestController {

    private final QuizService quizService;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final SystemLogService systemLogService;
    private final SessionRepository sessionRepository;
    private final QuizResultRepository quizResultRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Получение викторин только текущего авторизованного пользователя.
     */
    @GetMapping
    public List<Quiz> getAll(Principal principal) {
        log.info("Запрос на получение всех викторин для пользователя: {}", principal.getName());
        return quizRepository.findByAuthorUsername(principal.getName());
    }

    /**
     * Получение истории завершенных викторин.
     */
    @GetMapping("/history")
    public List<GameSession> getQuizHistory(Principal principal) {
        if (principal != null && principal.getName() != null) {
            log.info("[REST] Запрос истории для авторизованного преподавателя: {}", principal.getName());
            List<GameSession> history = sessionRepository.findByStatusAndQuizAuthorUsernameOrderByCreatedAtDesc("finished", principal.getName());

            if (history.isEmpty()) {
                return sessionRepository.findByStatusOrderByCreatedAtDesc("finished");
            }
            return history;
        }

        log.info("[REST] Анонимный запрос истории. Возвращаем все завершенные сессии системы.");
        return sessionRepository.findByStatusOrderByCreatedAtDesc("finished");
    }

    /**
     * ОБНОВЛЕНО: Прием ответов БЕЗ ИЗМЕНЕНИЯ структуры базы данных (Entity остается старой).
     * Текстовые ответы (тип text/multiple) прокидываются через query-параметр textAnswer
     * и улетают прямиком на веб-интерфейс преподавателя.
     * Эндпоинт: POST /api/quizzes/submit-answer
     */
    @PostMapping("/submit-answer")
    public ResponseEntity<QuizResult> submitAnswer(
            @RequestBody QuizResult result,
            @RequestParam(required = false) String textAnswer // Перехватываем текст без изменения полей Entity
    ) {
        String displayAnswer = (textAnswer != null) ? textAnswer : "ID вариантов: " + result.getAnswerId();

        log.info("[REST] Получен асинхронный ответ от студента '{}' для сессии ID: {}, Вопрос ID: {}, Ответ: '{}'",
                result.getStudentName(), result.getSessionId(), result.getQuizId(), displayAnswer);

        // 1. Железно сохраняем в старую базу (в поле answer_id запишется Integer, переданный мобилкой, например 0)
        QuizResult savedResult = quizResultRepository.save(result);

        // 2. Транслируем обновление прогресса на сайт преподавателя в реальном времени.
        String progressTopic = "/topic/session/" + result.getSessionId() + "/progress";

        messagingTemplate.convertAndSend(progressTopic, (Object) Map.of(
                "action", "STUDENT_PROGRESS",
                "studentName", result.getStudentName() != null ? result.getStudentName() : "Аноним",
                "userId", result.getUserId() != null ? result.getUserId() : 0,
                "quizId", result.getQuizId(),
                // На фронтенд шлем текстовое содержимое (если есть), иначе форматируем ID ответа
                "answerContent", (textAnswer != null) ? textAnswer : "Вариант №" + result.getAnswerId(),
                "isCorrect", result.getIsCorrect(),
                "timeSpent", result.getTimeSpent()
        ));

        log.info("[WEBSOCKET] Живой прогресс студента '{}' передан на фронтенд в топик: {}", result.getStudentName(), progressTopic);
        return ResponseEntity.ok(savedResult);
    }

    /**
     * Создание новой викторины с автоматической привязкой к автору.
     */
    @PostMapping("/create")
    public Quiz create(@RequestBody Quiz quiz, Principal principal, HttpServletRequest request) {
        log.info("Запрос на создание викторины '{}' от пользователя: {}", quiz.getTitle(), principal.getName());
        User author = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> {
                    log.error("Ошибка создания викторины: пользователь {} не найден", principal.getName());
                    return new RuntimeException("Текущий пользователь не найден в базе");
                });

        quiz.setAuthor(author);
        Quiz savedQuiz = quizService.saveFullQuiz(quiz);

        systemLogService.writeLog(author.getId(), "CREATE_QUIZ", "quiz", savedQuiz.getId(), request);
        return savedQuiz;
    }

    /**
     * Редактирование существующей викторины.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Quiz> update(@PathVariable Long id, @RequestBody Quiz quizDetails, Principal principal, HttpServletRequest request) {
        log.info("Запрос на обновление викторины с ID: {} от пользователя: {}", id, principal.getName());
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        return quizRepository.findById(id).map(quiz -> {
            if (!quiz.getAuthor().getUsername().equals(principal.getName())) {
                log.warn("Отказ в редактировании: пользователь {} не является автором викторины {}", principal.getName(), id);
                return ResponseEntity.status(403).<Quiz>build();
            }

            quizDetails.setId(id);
            quizDetails.setAuthor(quiz.getAuthor());

            Quiz updatedQuiz = quizService.saveFullQuiz(quizDetails);
            systemLogService.writeLog(user.getId(), "UPDATE_QUIZ", "quiz", updatedQuiz.getId(), request);

            return ResponseEntity.ok(updatedQuiz);
        }).orElseGet(() -> {
            log.warn("Викторина с ID: {} не найдена для обновления", id);
            return ResponseEntity.notFound().build();
        });
    }

    /**
     * Удаление викторины.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Principal principal, HttpServletRequest request) {
        log.info("Запрос на удаление викторины с ID: {} от пользователя: {}", id, principal.getName());
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        return quizRepository.findById(id).map(quiz -> {
            if (!quiz.getAuthor().getUsername().equals(principal.getName())) {
                log.warn("Отказ в удалении: пользователь {} не является автором викторины {}", principal.getName(), id);
                return ResponseEntity.status(403).body("У вас нет прав на удаление этой викторины");
            }

            quizRepository.delete(quiz);
            log.info("Викторина с ID: {} успешно удалена", id);

            systemLogService.writeLog(user.getId(), "DELETE_QUIZ", "quiz", id, request);
            return ResponseEntity.ok().build();
        }).orElseGet(() -> {
            log.warn("Викторина с ID: {} не найдена для удаления", id);
            return ResponseEntity.notFound().build();
        });
    }

    /**
     * Запуск игровой сессии (создание сессии в БД и выдача PIN).
     */
    @PostMapping("/{id}/start")
    public GameSession start(@PathVariable Long id, @RequestBody Map<String, String> body, Principal principal, HttpServletRequest request) {
        String pin = body.get("pin");
        log.info("Запуск игровой сессии для викторины ID: {} с PIN-кодом: {} от преподавателя: {}", id, pin, principal.getName());

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        GameSession session = quizService.createSession(id, pin);
        Long sessionIdAsLong = (session.getId() != null) ? session.getId().longValue() : null;

        systemLogService.writeLog(user.getId(), "START_GAME_SESSION", "game_session", sessionIdAsLong, request);
        return session;
    }

    /**
     * Успешное завершение сессии по PIN-коду (когда вопросы закончились).
     */
    @PostMapping("/sessions/{pin}/finish")
    public void finish(@PathVariable String pin, Principal principal, HttpServletRequest request) {
        log.info("Запрос на успешное завершение сессии по PIN: {} от пользователя: {}", pin, principal.getName());

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        quizService.finishSession(pin);
        systemLogService.writeLog(user.getId(), "FINISH_GAME_SESSION", "game_session", null, request);
    }

    /**
     * Принудительное прерывание/отмена сессии по PIN-коду (кнопка Отменить/Прервать).
     */
    @PostMapping("/sessions/{pin}/terminate")
    public ResponseEntity<?> terminateSession(@PathVariable String pin, Principal principal, HttpServletRequest request) {
        log.info("Принудительное прерывание активной сессии по PIN: {} от пользователя: {}", pin, principal.getName());

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        quizService.finishSession(pin);
        systemLogService.writeLog(user.getId(), "TERMINATE_GAME_SESSION", "game_session", null, request);

        return ResponseEntity.ok().build();
    }

    /**
     * Проверка PIN-кода для мобильного приложения (вход студента в викторину).
     */
    @GetMapping("/session/{pin}")
    public ResponseEntity<?> checkPin(@PathVariable String pin) {
        log.info("[МОБИЛЬНОЕ ПОДКЛЮЧЕНИЕ] Поступил запрос проверки PIN-кода: {}", pin);

        return quizService.findActiveSessionByPin(pin)
                .map(session -> {
                    log.info("Сессия для PIN: {} найдена. Текущий статус в БД: '{}'", pin, session.getStatus());

                    if ("finished".equals(session.getStatus())) {
                        log.warn("Подключение отклонено: сессия с PIN {} уже завершена", pin);
                        return ResponseEntity.badRequest()
                                .body(Map.of("message", "Эта викторина уже завершена"));
                    }

                    log.info("Студент успешно авторизован по PIN: {}. Доступ разрешен.", pin);
                    return ResponseEntity.ok(session);
                })
                .orElseGet(() -> {
                    log.error("Ошибка подключения: сессия с PIN-кодом {} вообще отсутствует в базе данных", pin);
                    return ResponseEntity.status(404)
                            .body(Map.of("message", "Викторина с таким PIN-кодом не найдена"));
                });
    }

    /**
     * Получение информации о сессии по её числовому ID.
     */
    @GetMapping("/session/id/{id}")
    public ResponseEntity<?> getSessionById(@PathVariable Integer id) {
        log.info("[МОБИЛЬНОЕ ПОДКЛЮЧЕНИЕ] Запрос данных игровой сессии по ID: {}", id);

        return quizService.findSessionById(id)
                .map(session -> {
                    log.info("Данные сессии с ID {} успешно отправлены на устройство студента", id);
                    return ResponseEntity.ok(session);
                })
                .orElseGet(() -> {
                    log.error("Запрос отклонен: игровая сессия с ID {} не существует в системе", id);
                    return ResponseEntity.notFound().build();
                });
    }
}