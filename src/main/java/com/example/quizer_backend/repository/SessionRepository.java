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
     *  Поиск всех сессий конкретного квиза с сортировкой от свежих к старым.
     */
    @EntityGraph(attributePaths = {"quiz"})
    List<GameSession> findByQuiz_IdOrderByCreatedAtDesc(Integer quizId);

    List<GameSession> findByStatusAndQuizAuthorUsernameOrderByCreatedAtDesc(String status, String username);

    List<GameSession> findByStatusAndQuizAuthorIdOrderByCreatedAtDesc(String status, Long authorId);

    /**
     * Показать вообще ВСЕ завершенные сессии в системе.
     */
    List<GameSession> findByStatusOrderByCreatedAtDesc(String status);
}