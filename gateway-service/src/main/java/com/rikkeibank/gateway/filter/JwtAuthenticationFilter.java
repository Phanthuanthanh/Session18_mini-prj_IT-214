package com.rikkeibank.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkeibank.common.constant.SecurityConstants;
import com.rikkeibank.common.dto.ErrorResponse;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh-token",
            "/actuator",
            "/eureka"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Allow open endpoints
        if (isOpenEndpoint(path)) {
            return chain.filter(exchange);
        }

        // Check Authorization header
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            log.warn("Missing Authorization Header for path: {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Khách vãng lai không có quyền truy cập dữ liệu tài chính. Vui lòng đăng nhập.");
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            log.warn("Invalid Authorization Header format: {}", authHeader);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Token không hợp lệ hoặc thiếu định dạng Bearer");
        }

        String token = authHeader.substring(SecurityConstants.TOKEN_PREFIX.length());

        try {
            if (!jwtTokenUtil.validateToken(token)) {
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Token đã hết hạn hoặc không hợp lệ");
            }

            Claims claims = jwtTokenUtil.getClaimsFromToken(token);
            Object userIdObj = claims.get("userId", Object.class);
            String userId = userIdObj != null ? String.valueOf(userIdObj) : "";
            String role = claims.get("role", String.class);
            String username = claims.getSubject();

            // Enforce RBAC rules at Gateway
            if (!isAuthorized(path, request.getMethod().name(), role)) {
                return onError(exchange, HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện hành động này (" + role + ")");
            }

            // Mutate request headers with user claims to downstream services
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(SecurityConstants.HEADER_USER_ID, userId)
                    .header(SecurityConstants.HEADER_USER_ROLE, role)
                    .header(SecurityConstants.HEADER_USER_NAME, username)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.error("Token verification failed: {}", e.getMessage());
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Xác thực token thất bại: " + e.getMessage());
        }
    }

    private boolean isOpenEndpoint(String path) {
        return OPEN_API_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private boolean isAuthorized(String path, String method, String role) {
        // ADMIN can access everything
        if ("ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            return true;
        }

        // Force logout is strictly ADMIN
        if (path.contains("/auth/force-logout")) {
            return false;
        }

        // Staff management is strictly ADMIN
        if (path.startsWith("/api/v1/staffs")) {
            return false;
        }

        // Customer modification (POST, DELETE) is ADMIN only
        if (path.startsWith("/api/v1/customers") && ("POST".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method))) {
            return false;
        }

        // Account Types modification is ADMIN only
        if (path.startsWith("/api/v1/account-types") && !("GET".equalsIgnoreCase(method))) {
            return false;
        }

        // TELLER specific endpoints
        if (path.contains("/teller/")) {
            return "ROLE_TELLER".equalsIgnoreCase(role) || "TELLER".equalsIgnoreCase(role);
        }

        return true;
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String errMessage) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(errMessage)
                .path(exchange.getRequest().getURI().getPath())
                .build();

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsString(errorResponse).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            bytes = ("{\"status\":" + status.value() + ",\"message\":\"" + errMessage + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
