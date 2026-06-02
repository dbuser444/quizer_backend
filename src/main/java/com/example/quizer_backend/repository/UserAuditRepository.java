package com.example.quizer_backend.repository;

import com.example.quizer_backend.entity.UserAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAuditRepository extends JpaRepository<UserAudit, Long> {
    List<UserAudit> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);
}