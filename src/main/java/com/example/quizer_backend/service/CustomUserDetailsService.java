package com.example.quizer_backend.service;

import com.example.quizer_backend.entity.User;
import com.example.quizer_backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // ВРЕМЕННО: Генерируем правильный хеш для 12345 прямо в коде
        String tempHash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("12345");
        System.out.println("ПРАВИЛЬНЫЙ ХЕШ ДЛЯ 12345: " + tempHash);

        User user = userRepository.findByUsername(email)
                                  .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User.builder()
                                                                 .username(user.getUsername())
                                                                 .password(user.getPassword())
                                                                 .authorities("ROLE_TEACHER")
                                                                 .build();
    }
}