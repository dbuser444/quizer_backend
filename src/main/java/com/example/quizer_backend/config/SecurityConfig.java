package com.example.quizer_backend.config;

import com.example.quizer_backend.config.IpBlacklistFilter; // Убедись, что импорт твоего фильтра корректен
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final IpBlacklistFilter ipBlacklistFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Настраиваем CORS фильтр так, чтобы он пропускал абсолютно всё, включая сокеты
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOriginPatterns(List.of("*"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(ipBlacklistFilter, UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Открываем все возможные пути сокетов для SockJS
                        .requestMatchers("/ws-quiz").permitAll()
                        .requestMatchers("/ws-quiz/**").permitAll()
                        .requestMatchers("/topic/**").permitAll()
                        .requestMatchers("/app/**").permitAll()

                        // Публичные REST эндпоинты
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/quizzes/session/**").permitAll()
                        .requestMatchers("/api/quizzes/sessions/**").permitAll()

                        // ИСПРАВЛЕНО: Явно открываем доступ для сохранения ответов с мобилки без авторизации
                        .requestMatchers("/api/quizzes/save-result").permitAll()

                        .requestMatchers("/api/sessions/**").permitAll()
                        .requestMatchers("/api/participants/**").permitAll()
                        .requestMatchers("/api/questions/**").permitAll()
                        .requestMatchers("/api/results/**").permitAll()

                        // Закрытые эндпоинты
                        .requestMatchers("/api/users/**").authenticated()
                        .anyRequest().authenticated()
                )
                // Глушим дефолтное модальное окно Basic Auth
                .httpBasic(basic -> basic.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}