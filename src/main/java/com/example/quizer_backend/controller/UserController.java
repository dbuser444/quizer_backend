package com.example.quizer_backend.controller;

import com.example.quizer_backend.entity.User;
import com.example.quizer_backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Получить данные текущего юзера
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                             .map(ResponseEntity::ok)
                             .orElse(ResponseEntity.status(404).build());
    }

    // Обновить профиль
    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> data, Principal principal) {
        return userRepository.findByUsername(principal.getName()).map(user -> {
            if (data.containsKey("fullName")) user.setFullName(data.get("fullName"));
            if (data.containsKey("email")) user.setUsername(data.get("email"));

            // Если прислали новый пароль — шифруем его
            if (data.containsKey("password") && !data.get("password").isEmpty()) {
                user.setPassword(passwordEncoder.encode(data.get("password")));
            }

            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Профиль обновлен"));
        }).orElse(ResponseEntity.status(404).build());
    }
}