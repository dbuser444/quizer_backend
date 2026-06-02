package com.example.quizer_backend.repository;

import com.example.quizer_backend.entity.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<Log, Long> {
    List<Log> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Log> findByActionOrderByCreatedAtDesc(String action);
}