package com.example.quizer_backend.controller;

import com.example.quizer_backend.dto.PlayerResult;
import com.example.quizer_backend.dto.QuizHistory;
import com.example.quizer_backend.entity.Quiz;
import com.example.quizer_backend.entity.QuizResult;
import com.example.quizer_backend.repository.QuizRepository;
import com.example.quizer_backend.repository.QuizResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST-контроллер для обработки и сохранения индивидуальных ответов студентов.
 */
@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
@Slf4j
public class QuizResultController {

    private final QuizResultRepository quizResultRepository;
    private final QuizRepository quizRepository;

    /**
     * Прием ответов от мобильного приложения
     * Добавлено логирование для отслеживания шагов мобильного клиента.
     */
    @PostMapping("/save-result")
    public ResponseEntity<Void> saveResult(@RequestBody QuizResult quizResult) {
        log.info("=================== [МОБИЛЬНЫЙ ВХОД] ===================");
        log.info(" Студент стучится в базу: '{}'", quizResult.getStudentName());
        log.info(" ID Сессии (SessionId):   {}", quizResult.getSessionId());
        log.info(" ID Квиза/Теста (QuizId):  {}", quizResult.getQuizId());
        log.info(" ID Вопроса (QuestionId): {}", quizResult.getQuestionId());
        log.info(" Выбранный ответ (AnswerId): {}", quizResult.getAnswerId() != null ? quizResult.getAnswerId() : "ПРОПУСК ВОПРОСА");
        log.info(" Время на размышление:    {} сек.", quizResult.getTimeSpent());
        log.info(" Вердикт приложения:      {}", quizResult.getIsCorrect() != null && quizResult.getIsCorrect() ? " ВЕРНО" : " НЕВЕРНО");
        log.info("----------------------------------------------------------");

        try {
            // Защита от пустых данных
            if (quizResult.getSessionId() == null || quizResult.getStudentName() == null || quizResult.getQuestionId() == null) {
                log.warn("[ВНИМАНИЕ] Мобилка прислала пустые критические поля (sessionId, studentName или questionId)!");
            }

            QuizResult savedResult = quizResultRepository.save(quizResult);

            // ЛОГ УСПЕШНОЙ ЗАПИСИ
            log.info("[УСПЕХ] Данные сохранены в таблицу 'results'. Присвоен ID записи: {}", savedResult.getId());
            log.info("==========================================================\n");
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            // ЛОГ ОШИБКИ БАЗЫ ДАННЫХ
            log.error("[ОШИБКА БД] Не удалось записать ответ студента в PostgreSQL!");
            log.error("Текст ошибки: {}", e.getMessage());
            log.error("==========================================================\n");
            return ResponseEntity.internalServerError().build();
        }


    }
    @GetMapping("/session/{sessionId}/results")
    public ResponseEntity<List<PlayerResult>> getResults(@PathVariable Integer sessionId) {
        List<QuizResult> allAnswers = quizResultRepository.findBySessionId(sessionId);

        // Группируем ответы по студенту и считаем баллы
        Map<String, List<QuizResult>> grouped = allAnswers.stream()
                .collect(Collectors.groupingBy(QuizResult::getStudentName));

        List<PlayerResult> results = grouped.entrySet().stream().map(entry -> {
            int totalScore = (int) entry.getValue().stream()
                    .filter(ans -> Boolean.TRUE.equals(ans.getIsCorrect()))
                    .count() * 10; // Допустим, 10 баллов за ответ
            double time = entry.getValue().stream().mapToDouble(QuizResult::getTimeSpent).sum();
            return new PlayerResult(entry.getKey(), totalScore, (float) time);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    @GetMapping("/users/{userId}/history")
    public ResponseEntity<List<QuizHistory>> getUserHistory(@PathVariable Long userId) {
        List<QuizResult> userResults = quizResultRepository.findByUserId(userId.intValue());

        return ResponseEntity.ok(
                userResults.stream()
                        .collect(Collectors.groupingBy(QuizResult::getSessionId))
                        .entrySet().stream()
                        .sorted((e1, e2) -> e2.getKey().compareTo(e1.getKey()))
                        .map(entry -> {
                            List<QuizResult> results = entry.getValue();
                            // Приводим Integer к Long, чтобы найти в QuizRepository
                            Long quizId = results.get(0).getQuizId().longValue();

                            // Запрос к базе данных за названием викторины
                            String title = quizRepository.findById(quizId)
                                    .map(Quiz::getTitle)
                                    .orElse("Викторина #" + quizId);

                            int total = results.size();
                            int correct = (int) results.stream().filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
                            int scorePercent = (total > 0) ? (correct * 100 / total) : 0;

                            return new QuizHistory(
                                    title,
                                    "Завершена",
                                    scorePercent,
                                    total,
                                    correct
                            );
                        })
                        .collect(Collectors.toList())
        );
    }
    @GetMapping("/users/{userId}/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable Long userId) {
        log.info("Запрос статистики для userId: {}", userId);
        List<QuizResult> allResults = quizResultRepository.findByUserId(userId.intValue());

        // ЛОГ ДЛЯ ОТЛАДКИ
        log.info("Найдено результатов в базе для пользователя: {}", allResults.size());

        if (allResults.isEmpty()) {
            log.warn("Статистика пуста для пользователя {}", userId);
            Map<String, Object> emptyStats = new HashMap<>();
            emptyStats.put("totalQuizzes", 0);
            emptyStats.put("totalPoints", 0);
            emptyStats.put("avgScore", 0);
            emptyStats.put("bestScore", 0);
            return ResponseEntity.ok(emptyStats);
        }

        // 1. Количество уникальных сессий
        long totalQuizzes = allResults.stream()
                .map(QuizResult::getSessionId)
                .filter(id -> id != null) // Защита от null сессий
                .distinct()
                .count();

        // 2. Общее количество очков
        int totalPoints = (int) allResults.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsCorrect()))
                .count() * 10;

        // 3. Группировка
        Map<Integer, List<QuizResult>> resultsBySession = allResults.stream()
                .filter(r -> r.getSessionId() != null)
                .collect(Collectors.groupingBy(QuizResult::getSessionId));

        List<Integer> sessionScores = resultsBySession.values().stream().map(results -> {
            long correctCount = results.stream().filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
            return (int) (correctCount * 100 / results.size());
        }).collect(Collectors.toList());

        int avgScore = (int) sessionScores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        int bestScore = sessionScores.stream().mapToInt(Integer::intValue).max().orElse(0);

        log.info("Расчет статистики: Игр={}, Очков={}, Средний={}, Рекорд={}", totalQuizzes, totalPoints, avgScore, bestScore);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQuizzes", (int) totalQuizzes);
        stats.put("totalPoints", totalPoints);
        stats.put("avgScore", avgScore);
        stats.put("bestScore", bestScore);

        return ResponseEntity.ok(stats);
    }


}