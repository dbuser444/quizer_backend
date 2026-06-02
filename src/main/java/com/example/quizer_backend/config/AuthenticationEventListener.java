package com.example.quizer_backend.config;

import com.example.quizer_backend.service.SecurityAnomaliesService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationEventListener {

    private final SecurityAnomaliesService securityAnomaliesService;
    private final HttpServletRequest request;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        String ipAddress = request.getRemoteAddr();
        securityAnomaliesService.registerLoginAttempt(username, true, ipAddress);
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        String ipAddress = request.getRemoteAddr();
        securityAnomaliesService.registerLoginAttempt(username, false, ipAddress);
    }
}