package com.example.quizer_backend.repository;

import com.example.quizer_backend.entity.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuizResultRepository extends JpaRepository<QuizResult, Integer> {
    // Метод для вытаскивания всех ответов по ID сессии
    List<QuizResult> findBySessionId(Integer sessionId);
}