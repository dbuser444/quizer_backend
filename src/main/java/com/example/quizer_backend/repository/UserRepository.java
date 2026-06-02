package com.example.quizer_backend.repository;

import com.example.quizer_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Используем username, так как это имя поля в твоем классе User
    Optional<User> findByUsername(String username);
}