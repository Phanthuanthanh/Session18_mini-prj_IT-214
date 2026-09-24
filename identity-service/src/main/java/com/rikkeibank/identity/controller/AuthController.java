package com.rikkeibank.identity.controller;

import com.rikkeibank.common.constant.SecurityConstants;
import com.rikkeibank.common.dto.ApiResponse;
import com.rikkeibank.common.exception.ForbiddenException;
import com.rikkeibank.identity.dto.AuthResponse;
import com.rikkeibank.identity.dto.LoginRequest;
import com.rikkeibank.identity.dto.RefreshTokenRequest;
import com.rikkeibank.identity.dto.RegisterRequest;
import com.rikkeibank.identity.entity.User;
import com.rikkeibank.identity.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Đăng ký tài khoản thành công", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = SecurityConstants.HEADER_AUTHORIZATION, required = false) String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }

    @PostMapping("/force-logout/{userId}")
    public ResponseEntity<ApiResponse<Void>> forceLogout(
            @PathVariable Long userId,
            @RequestHeader(value = SecurityConstants.HEADER_USER_ROLE, required = false) String role) {
        if (role == null || !role.contains("ADMIN")) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền ép buộc người dùng đăng xuất");
        }
        authService.forceLogout(userId);
        return ResponseEntity.ok(ApiResponse.success("Đã ép buộc đăng xuất và thu hồi mọi quyền truy cập của người dùng ID: " + userId, null));
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(
            @RequestParam("token") String token) {
        boolean isValid = authService.validateToken(token);
        return ResponseEntity.ok(ApiResponse.success(isValid));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers(
            @RequestHeader(value = SecurityConstants.HEADER_USER_ROLE, required = false) String role) {
        if (role == null || !role.contains("ADMIN")) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền xem danh sách người dùng");
        }
        return ResponseEntity.ok(ApiResponse.success(authService.getAllUsers()));
    }
}
