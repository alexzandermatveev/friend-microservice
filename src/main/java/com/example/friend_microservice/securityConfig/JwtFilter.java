package com.example.friend_microservice.securityConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final WebClient webClient;
    private final DiscoveryClient discoveryClient;
    @Value("${serviceAuth.service-name}")
    private String serviceName;
    @Value("${serviceAuth.pathTo}")
    private String serviceAuthPath;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            // Проверка токена через auth-service
            boolean isValid = validateTokenWithAuthService(token);

            if (isValid) {
                String userId = extractUserIdFromToken(token); // парсим токен и извлекаем userId
                if (userId.isEmpty()) {
                    log.error("can`t extract user id from token: {}", token);
                    SecurityContextHolder.clearContext();
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT Token");
                    return;
                }
                // создаем объект аутентификации
                var authToken = new JwtAuthenticationToken(userId, token);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
//            // если микросервис отправил false - выбрасываем ошибку
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
                return;
            }
        }
        // передаем следующему фильтру
        filterChain.doFilter(request, response);
    }

    public boolean validateTokenWithAuthService(String token) {
        //заглушка
        if (serviceAuthPath.equals("test")) return true;
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
            if (instances.isEmpty()) {
                throw new RuntimeException("Auth service not available in Eureka!");
            }
            log.debug("use auth service with uri: {}", instances.get(0).getUri().toString());
            String authServiceUrl = String.format("%s%s%s", instances.get(0).getUri().toString(), serviceAuthPath, token);

            // Отправляем запрос для проверки токена
            Boolean response = webClient.post()
                    .uri(authServiceUrl)
                    .header("Authorization", "Bearer " + token) // Если сервис требует токен в заголовке
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block(); // Синхронное выполнение запроса

            return response != null && response;
        } catch (Exception e) {
            log.debug(e.getMessage());
            return false; // Если произошла ошибка, токен считается невалидным
        }
    }

    private String extractUserIdFromToken(String token) {
        // распаковываем JWT и извлекаем userId
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // предполагается, что шифруется base64
            String payload = new String(Base64.getDecoder().decode(token.split("\\.")[1]));
            Map<String, Object> body = objectMapper.readValue(payload, Map.class);
            return body.get("sub").toString();
        } catch (Exception e) {
            return "";
//            throw new IllegalArgumentException("Wrong JWT: ", e);
        }
    }
}
