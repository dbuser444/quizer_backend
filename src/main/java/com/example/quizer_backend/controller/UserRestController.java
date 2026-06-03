package com.example.quizer_backend.controller;

import com.example.quizer_backend.entity.User;
import com.example.quizer_backend.repository.UserRepository;
import com.example.quizer_backend.service.UserAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
@Slf4j
public class UserRestController {

    private final UserRepository userRepository;
    private final UserAuditService userAuditService;

    /**
     * Получение профиля текущего авторизованного преподавателя
     */
    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(Principal principal) {
        log.info("Запрос профиля пользователя: {}", principal.getName());
        return userRepository.findByUsername(principal.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Обновление данных профиля с фиксацией изменений в user_audit
     */
    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(@RequestBody User updatedData, Principal principal) {
        log.info("Запрос на обновление профиля от пользователя: {}", principal.getName());

        // 1. Ищем текущего пользователя
        User currentUser = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Авторизованный пользователь не найден"));

        // 2. Создаем копию старого состояния
        User oldStateCopy = new User();
        oldStateCopy.setId(currentUser.getId());
        oldStateCopy.setFullName(currentUser.getFullName());
        oldStateCopy.setUsername(currentUser.getUsername());
        oldStateCopy.setRole(currentUser.getRole());

        // 3.  Обновляем поля сущности
        if (updatedData.getFullName() != null) {
            currentUser.setFullName(updatedData.getFullName());
        }
        if (updatedData.getUsername() != null) {
            currentUser.setUsername(updatedData.getUsername());
        }

        // Сохраняем обновленного пользователя в БД
        User savedUser = userRepository.save(currentUser);

        // 4. ОТПРАВКА ДАННЫХ В СИСТЕМНЫЙ МОДУЛЬ public.user_audit
        userAuditService.auditUserChanges(oldStateCopy, savedUser, currentUser.getId());

        return ResponseEntity.ok(savedUser);
    }
}