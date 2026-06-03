package com.example.quizer_backend.controller;

import com.example.quizer_backend.entity.QuizResult;
import com.example.quizer_backend.repository.QuizResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST-контроллер для обработки и сохранения индивидуальных ответов студентов.
 */
@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
@Slf4j
public class QuizResultController {

    private final QuizResultRepository quizResultRepository;

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
}