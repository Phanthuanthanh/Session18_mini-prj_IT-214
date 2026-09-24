package com.rikkeibank.identity.service;

import com.rikkeibank.common.enums.UserRole;
import com.rikkeibank.common.exception.BadRequestException;
import com.rikkeibank.common.exception.ResourceNotFoundException;
import com.rikkeibank.common.exception.UnauthorizedException;
import com.rikkeibank.identity.dto.AuthResponse;
import com.rikkeibank.identity.dto.LoginRequest;
import com.rikkeibank.identity.dto.RefreshTokenRequest;
import com.rikkeibank.identity.dto.RegisterRequest;
import com.rikkeibank.identity.entity.User;
import com.rikkeibank.identity.repository.UserRepository;
import com.rikkeibank.identity.security.JwtProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Tên đăng nhập '" + request.getUsername() + "' đã tồn tại");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' đã được sử dụng");
        }

        UserRole role = request.getRole() != null ? request.getRole() : UserRole.CUSTOMER;

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(role)
                .status("ACTIVE")
                .build();

        user = userRepository.save(user);
        log.info("New user registered successfully: id={}, username={}, role={}", user.getId(), user.getUsername(), user.getRole());

        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProvider.getAccessTokenExpiration() / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Tên đăng nhập hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new UnauthorizedException("Tài khoản đã bị khóa hoặc ngừng hoạt động");
        }

        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        log.info("User logged in successfully: username={}, role={}", user.getUsername(), user.getRole());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProvider.getAccessTokenExpiration() / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Refresh token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.");
        }

        if (tokenBlacklistService.isTokenBlacklisted(refreshToken)) {
            throw new UnauthorizedException("Refresh token này đã bị thu hồi");
        }

        Claims claims = jwtProvider.getClaimsFromToken(refreshToken);
        String username = claims.getSubject();
        long issuedAt = claims.getIssuedAt().getTime();
        Object userIdObj = claims.get("userId");
        Long userId = userIdObj != null ? Long.valueOf(userIdObj.toString()) : null;

        if (tokenBlacklistService.isUserForceLoggedOut(userId, issuedAt)) {
            throw new UnauthorizedException("Phiên làm việc đã bị Admin thu hồi quyền truy cập. Vui lòng đăng nhập lại.");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new UnauthorizedException("Tài khoản người dùng đã bị khóa");
        }

        String newAccessToken = jwtProvider.generateAccessToken(user);
        String newRefreshToken = jwtProvider.generateRefreshToken(user);

        // Invalidate old refresh token (Token rotation for security)
        tokenBlacklistService.blacklistToken(refreshToken, 2592000000L);

        log.info("Token refreshed successfully for username={}", username);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProvider.getAccessTokenExpiration() / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        tokenBlacklistService.blacklistToken(token, 3600000L);
        log.info("User logged out, token added to blacklist.");
    }

    @Transactional
    public void forceLogout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        user.setMinValidTokenTimestamp(System.currentTimeMillis());
        userRepository.save(user);

        tokenBlacklistService.forceLogoutUser(userId);
        log.warn("ADMIN FORCED LOGOUT for user id={}, username={}. All active sessions invalidated.", user.getId(), user.getUsername());
    }

    public boolean validateToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (!jwtProvider.validateToken(token) || tokenBlacklistService.isTokenBlacklisted(token)) {
            return false;
        }

        try {
            Claims claims = jwtProvider.getClaimsFromToken(token);
            Object userIdObj = claims.get("userId");
            Long userId = userIdObj != null ? Long.valueOf(userIdObj.toString()) : null;
            long issuedAt = claims.getIssuedAt().getTime();
            return !tokenBlacklistService.isUserForceLoggedOut(userId, issuedAt);
        } catch (Exception e) {
            return false;
        }
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
