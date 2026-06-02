package com.example.quizer_backend.service;

import com.example.quizer_backend.entity.GameSession;
import com.example.quizer_backend.entity.Quiz;
import com.example.quizer_backend.repository.QuizRepository;
import com.example.quizer_backend.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {

    private final QuizRepository quizRepository;
    private final SessionRepository sessionRepository;

    /**
     * Сохранение викторины со всеми вложенными вопросами и ответами.
     */
    @Transactional
    public Quiz saveFullQuiz(Quiz quiz) {
        if (quiz.getId() != null) {
            log.info("Сервис: обновление существующей викторины с ID: {}", quiz.getId());
            Quiz existingQuiz = quizRepository.findById(quiz.getId())
                    .orElseThrow(() -> {
                        log.error("Сервис: Викторина с ID {} не найдена", quiz.getId());
                        return new NoSuchElementException("Quiz not found");
                    });

            existingQuiz.setTitle(quiz.getTitle());
            existingQuiz.setDescription(quiz.getDescription());
            existingQuiz.setImageUrl(quiz.getImageUrl());
            existingQuiz.setIsPublic(quiz.getIsPublic());

            existingQuiz.getQuestions().clear();
            if (quiz.getQuestions() != null) {
                quiz.getQuestions().forEach(q -> {
                    q.setQuiz(existingQuiz);
                    if (q.getAnswers() != null) {
                        q.getAnswers().forEach(a -> a.setQuestion(q));
                    }
                    existingQuiz.getQuestions().add(q);
                });
            }
            return quizRepository.save(existingQuiz);
        } else {
            log.info("Сервис: создание абсолютно новой викторины '{}'", quiz.getTitle());
            if (quiz.getQuestions() != null) {
                quiz.getQuestions().forEach(q -> {
                    q.setQuiz(quiz);
                    if (q.getAnswers() != null) {
                        q.getAnswers().forEach(a -> a.setQuestion(q));
                    }
                });
            }
            return quizRepository.save(quiz);
        }
    }

    /**
     * Создание новой игровой сессии.
     */
    @Transactional
    public GameSession createSession(Long quizId, String pin) {
        log.info("Сервис: генерация сессии в БД для викторины ID: {}, сгенерированный PIN: {}", quizId, pin);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> {
                    log.error("Сервис: не удалось создать сессию. Викторина ID {} не найдена", quizId);
                    return new NoSuchElementException("Викторина с ID " + quizId + " не найдена");
                });

        GameSession session = new GameSession();
        session.setQuiz(quiz);
        session.setPinCode(pin);
        session.setStatus("active");
        session.setStartedAt(LocalDateTime.now());

        GameSession savedSession = sessionRepository.save(session);
        log.info("Сервис: сессия успешно сохранена в БД с ID: {}, статус: {}, запущен в: {}",
                savedSession.getId(), savedSession.getStatus(), savedSession.getStartedAt());
        return savedSession;
    }

    /**
     * Завершение сессии по PIN-коду.
     * ИСПРАВЛЕНО: Теперь при закрытии игры проставляется точное время финиша.
     */
    @Transactional
    public void finishSession(String pin) {
        log.info("Сервис: перевод сессии {} в статус 'finished'", pin);

        sessionRepository.findByPinCode(pin).ifPresentOrElse(session -> {
            session.setStatus("finished");
            session.setFinishedAt(LocalDateTime.now()); // КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ: Пишем время окончания!
            sessionRepository.save(session);
            log.info("Сервис: статус сессии {} изменен на 'finished'. Время закрытия: {}", pin, session.getFinishedAt());
        }, () -> log.warn("Сервис: не удалось завершить сессию. Сессия с PIN {} не найдена в БД", pin));
    }

    /**
     * Поиск активной игровой сессии по PIN-коду для мобильного приложения и веб-панели.
     * Благодаря настроенному @EntityGraph в репозитории, этот метод вернет сессию
     * сразу вместе со связанным Quiz и списком Questions за один запрос.
     */
    @Transactional(readOnly = true)
    public Optional<GameSession> findActiveSessionByPin(String pin) {
        log.info("Сервис: обращение к репозиторию для поиска сессии по коду: {}", pin);
        return sessionRepository.findByPinCode(pin);
    }

    /**
     * Поиск сессии по её уникальному идентификатору (ID).
     */
    @Transactional(readOnly = true)
    public Optional<GameSession> findSessionById(Integer id) {
        log.info("Сервис: обращение к репозиторию для поиска сессии по ID: {}", id);
        return sessionRepository.findById(id);
    }
}