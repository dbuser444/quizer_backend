package com.example.quizer_backend.repository;

import com.example.quizer_backend.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByAuthorUsername(String username);
}