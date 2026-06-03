package com.example.quizer_backend.repository;

import com.example.quizer_backend.entity.SessionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, Integer> {

    List<SessionParticipant> findBySessionId(Integer sessionId);
}