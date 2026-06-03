package com.example.quizer_backend.service;

import com.example.quizer_backend.entity.User;
import com.example.quizer_backend.entity.UserAudit;
import com.example.quizer_backend.repository.UserAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAuditService {

    private final UserAuditRepository userAuditRepository;

    /**
     * Сравнивает старое и новое состояние пользователя и записывает различия в аудит
     */
    @Transactional
    public void auditUserChanges(User oldUser, User newUser, Long changedById) {
        try {
            // 1. Проверяем изменение ФИО
            if (!Objects.equals(oldUser.getFullName(), newUser.getFullName())) {
                saveAuditLog(oldUser.getId(), changedById, "full_name", oldUser.getFullName(), newUser.getFullName());
            }

            // 2. Проверяем изменение Логина/Email
            if (!Objects.equals(oldUser.getUsername(), newUser.getUsername())) {
                saveAuditLog(oldUser.getId(), changedById, "username", oldUser.getUsername(), newUser.getUsername());
            }

            // 3. Проверяем изменение Роли
            if (!Objects.equals(oldUser.getRole(), newUser.getRole())) {
                saveAuditLog(oldUser.getId(), changedById, "role", oldUser.getRole(), newUser.getRole());
            }

        } catch (Exception e) {
            log.error("Ошибка при генерации логов аудита пользователя: ", e);
        }
    }

    private void saveAuditLog(Long targetUserId, Long changedById, String field, String oldVal, String newVal) {
        UserAudit auditEntry = UserAudit.builder()
                .targetUserId(targetUserId)
                .changedBy(changedById)
                .fieldChanged(field)
                .oldValue(oldVal)
                .newValue(newVal)
                .build();

        userAuditRepository.save(auditEntry);
        log.info("[АУДИТ ПОЛЕЙ] Изменено поле '{}' у пользователя ID {}. Было: '{}', Стало: '{}'", field, targetUserId, oldVal, newVal);
    }
}