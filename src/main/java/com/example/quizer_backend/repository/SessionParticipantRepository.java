package com.example.quizer_backend.repository;

import com.example.quizer_backend.entity.SessionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, Integer> {

    /**
     * ДОБАВЛЕНО: Находит всех студентов (участников), подключенных к конкретной сессии.
     * Spring Data JPA автоматически сгенерирует SQL-запрос на основе этого имени метода.
     */
    List<SessionParticipant> findBySessionId(Integer sessionId);
}