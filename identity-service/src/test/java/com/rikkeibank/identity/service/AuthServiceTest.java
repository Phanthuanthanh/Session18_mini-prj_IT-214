package com.rikkeibank.identity.service;

import com.rikkeibank.common.enums.UserRole;
import com.rikkeibank.common.exception.BadRequestException;
import com.rikkeibank.common.exception.UnauthorizedException;
import com.rikkeibank.identity.dto.AuthResponse;
import com.rikkeibank.identity.dto.LoginRequest;
import com.rikkeibank.identity.dto.RegisterRequest;
import com.rikkeibank.identity.entity.User;
import com.rikkeibank.identity.repository.UserRepository;
import com.rikkeibank.identity.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("encoded_pass")
                .email("test@example.com")
                .role(UserRole.CUSTOMER)
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("Register new user successfully")
    void testRegister_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .password("password123")
                .email("new@example.com")
                .role(UserRole.CUSTOMER)
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtProvider.generateAccessToken(any(User.class))).thenReturn("access_token");
        when(jwtProvider.generateRefreshToken(any(User.class))).thenReturn("refresh_token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
    }

    @Test
    @DisplayName("Register fails when username already exists")
    void testRegister_UsernameExists() {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .password("password123")
                .email("new@example.com")
                .build();

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }

    @Test
    @DisplayName("Login successfully with valid credentials")
    void testLogin_Success() {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("raw_pass")
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("raw_pass", "encoded_pass")).thenReturn(true);
        when(jwtProvider.generateAccessToken(mockUser)).thenReturn("access_token");
        when(jwtProvider.generateRefreshToken(mockUser)).thenReturn("refresh_token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
    }

    @Test
    @DisplayName("Login fails with wrong password")
    void testLogin_WrongPassword() {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("wrong_pass")
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrong_pass", "encoded_pass")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Force logout invokes token revocation")
    void testForceLogout_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        authService.forceLogout(1L);

        verify(tokenBlacklistService, times(1)).forceLogoutUser(1L);
        verify(userRepository, times(1)).save(mockUser);
    }
}
