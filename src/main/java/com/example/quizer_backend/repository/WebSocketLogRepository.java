package com.example.quizer_backend.repository;

import com.example.quizer_backend.entity.WebSocketLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebSocketLogRepository extends JpaRepository<WebSocketLog, Long> {
    List<WebSocketLog> findBySessionIdOrderByCreatedAtDesc(Integer sessionId);
}