package com.example.quizer_backend.repository;

import com.example.quizer_backend.entity.IpBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IpBlacklistRepository extends JpaRepository<IpBlacklist, Long> {
    Optional<IpBlacklist> findByIpAddress(String ipAddress);

    // Проверка, заблокирован ли IP на текущий момент
    boolean existsByIpAddressAndExpiresAtAfter(String ipAddress, LocalDateTime now);
}