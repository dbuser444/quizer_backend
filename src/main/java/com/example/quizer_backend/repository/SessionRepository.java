package com.example.quizer_backend.repository;

import com.example.quizer_backend.entity.GameSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<GameSession, Integer> {

    @EntityGraph(attributePaths = {"quiz", "quiz.questions"})
    Optional<GameSession> findByPinCode(String pinCode);

    @EntityGraph(attributePaths = {"quiz", "quiz.questions"})
    Optional<GameSession> findById(Integer id);

    /**
     * ИСПРАВЛЕНИЕ СОРТИРОВКИ: Поиск всех сессий конкретного квиза с сортировкой от свежих к старым.
     * Благодаря этому вчерашние и сегодняшние игры будут на самом верху списка.
     */
    @EntityGraph(attributePaths = {"quiz"})
    List<GameSession> findByQuiz_IdOrderByCreatedAtDesc(Integer quizId);

    /**
     * Старый метод (по username автора) — оставляем, чтобы ничего не сломать на фронтенде
     */
    List<GameSession> findByStatusAndQuizAuthorUsernameOrderByCreatedAtDesc(String status, String username);

    /**
     * Поиск истории по ID преподавателя.
     * Числа сравнивать гораздо безопаснее, чем строки с email.
     */
    List<GameSession> findByStatusAndQuizAuthorIdOrderByCreatedAtDesc(String status, Long authorId);

    /**
     * Показать вообще ВСЕ завершенные сессии в системе.
     * Если вызвать его, ты сразу увидишь, попадают ли игры в историю в принципе.
     */
    List<GameSession> findByStatusOrderByCreatedAtDesc(String status);
}