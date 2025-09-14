package com.example.cloudstorage.config;

import com.example.cloudstorage.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    public TokenAuthenticationFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);
            String requestPath = request.getRequestURI();

            // Пропускаем публичные endpoints без проверки токена
            if (isPublicEndpoint(requestPath)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (token != null && tokenService.validateToken(token)) {
                String username = tokenService.getUsernameFromToken(token);

                if (username != null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, List.of());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else if (token != null) {
                // Токен есть, но невалидный
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("{\"error\": \"Invalid token\"}");
                return;
            } else if (!isPublicEndpoint(requestPath)) {
                // Нет токена для защищенного endpoint
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("{\"error\": \"Authentication required\"}");
                return;
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.getWriter().write("{\"error\": \"Internal server error\"}");
        }
    }

    private boolean isPublicEndpoint(String path) {
        return path.equals("/cloud/actuator/health") ||
                path.equals("/cloud/actuator/info") ||
                path.equals("/cloud/login") ||
                path.equals("/cloud/register") ||
                path.equals("/cloud/logout") ||
                path.startsWith("/cloud/actuator/");
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        // Проверяем заголовок Authorization
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Проверяем кастомный заголовок
        String authToken = request.getHeader("auth-token");
        if (authToken != null) {
            return authToken;
        }

        return null;
    }
}