package com.pawlingo.api.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pawlingo.api.auth.dto.request.LoginRequest;
import com.pawlingo.api.auth.dto.request.RegisterRequest;
import com.pawlingo.api.auth.dto.response.LoginResponse;
import com.pawlingo.api.auth.dto.response.RegisterResponse;
import com.pawlingo.api.auth.service.JwtService;
import com.pawlingo.api.common.exception.BusinessException;
import com.pawlingo.api.common.exception.ErrorCode;
import com.pawlingo.api.user.AuthProvider;
import com.pawlingo.api.user.Goal;
import com.pawlingo.api.user.User;
import com.pawlingo.api.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void register_newEmail_createsUserAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("user@example.com", "password123", null);
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(jwtService.generateToken(any(UUID.class), anyString())).thenReturn("jwt-token");

        RegisterResponse response = authService.register(request);

        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.goal()).isEqualTo(Goal.BEGINNER);
        assertThat(response.accessToken()).isEqualTo("jwt-token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsBusinessExceptionWithDuplicateEmailCode() {
        RegisterRequest request = new RegisterRequest("user@example.com", "password123", null);
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    void login_correctCredentials_returnsAccessToken() {
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("hashed")
                .goal(Goal.BEGINNER)
                .authProvider(AuthProvider.LOCAL)
                .build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(user.getId(), user.getEmail())).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);

        LoginResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.expiresIn()).isEqualTo(86400L);
    }

    @Test
    void login_wrongPassword_throwsBusinessExceptionWithInvalidCredentialsCode() {
        LoginRequest request = new LoginRequest("user@example.com", "wrong-password");
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("hashed")
                .goal(Goal.BEGINNER)
                .authProvider(AuthProvider.LOCAL)
                .build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void login_unknownEmail_throwsBusinessExceptionWithInvalidCredentialsCode() {
        LoginRequest request = new LoginRequest("missing@example.com", "password123");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }
}
