package com.example.quizer_backend.config;

import com.example.quizer_backend.repository.IpBlacklistRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class IpBlacklistFilter extends OncePerRequestFilter {

    private final IpBlacklistRepository ipBlacklistRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1. ЗЕЛЕНЫЙ КОРИДОР ДЛЯ WEBSOCKET: сокеты фильтр вообще не трогает
        if (path != null && path.startsWith("/ws-quiz")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = request.getRemoteAddr();

        // 2. ЗАЩИТА РАЗРАБОТЧИКА: игнорируем локальный хост (IPv4 и IPv6 петли), чтобы не ломать тесты
        if ("127.0.0.1".equals(clientIp) || "0:0:0:0:0:0:0:1".equals(clientIp) || "::1".equals(clientIp)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 3. Проверяем, находится ли реальный IP в таблице активных банов
            boolean isBanned = ipBlacklistRepository.existsByIpAddressAndExpiresAtAfter(clientIp, LocalDateTime.now());

            if (isBanned) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Доступ ограничен. Ваш IP-адрес временно заблокирован подсистемой безопасности.\"}");
                return; // Прерываем обработку запроса
            }
        } catch (Exception e) {
            // Если в базе произошел сбой при парсинге IP — логируем, но не ломаем приложение
            logger.error("Ошибка проверки IP в черном списке: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}