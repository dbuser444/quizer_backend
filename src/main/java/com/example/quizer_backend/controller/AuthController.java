package com.example.quizer_backend.controller;

import com.example.quizer_backend.dto.RegisterRequest;
import com.example.quizer_backend.entity.User;
import com.example.quizer_backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth") // Это общий корень для всех методов в классе
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // Проверка Email
        if (userRepository.findByUsername(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest()
                                 .body(Map.of("message", "Пользователь с таким Email уже существует"));
        }

        User user = new User();
        user.setUsername(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        if ("ROLE_TEACHER".equals(request.getRole())) {
            user.setRole("ROLE_TEACHER");
        } else {
            user.setRole("ROLE_STUDENT");
        }

        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Регистрация прошла успешно",
                "role", user.getRole()
                                       ));
    }

    // Этот метод теперь будет доступен по адресу /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login() {
        return ResponseEntity.ok(Map.of("message", "Вы успешно вошли в систему"));
    }
}