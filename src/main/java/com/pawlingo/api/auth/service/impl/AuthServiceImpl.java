package com.pawlingo.api.auth.service.impl;

import com.pawlingo.api.auth.dto.request.LoginRequest;
import com.pawlingo.api.auth.dto.request.RegisterRequest;
import com.pawlingo.api.auth.dto.response.LoginResponse;
import com.pawlingo.api.auth.dto.response.MeResponse;
import com.pawlingo.api.auth.dto.response.RegisterResponse;
import com.pawlingo.api.auth.service.AuthService;
import com.pawlingo.api.auth.service.JwtService;
import com.pawlingo.api.common.exception.BusinessException;
import com.pawlingo.api.common.exception.ErrorCode;
import com.pawlingo.api.user.entity.User;
import com.pawlingo.api.user.enums.AuthProvider;
import com.pawlingo.api.user.enums.Goal;
import com.pawlingo.api.user.repository.UserRepository;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL, "Email already registered: " + request.email());
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .goal(request.goal() != null ? request.goal() : Goal.BEGINNER)
                .authProvider(AuthProvider.LOCAL)
                .build();
        user = userRepository.save(user);

        // TODO (see auth-spec.md#data-model): auto-create a default Pet for this user
        // once the Pet entity/repository exists. Intentionally a no-op until then so
        // auth can ship without blocking on the Pet module.

        String accessToken = jwtService.generateToken(user.getId(), user.getEmail());
        return new RegisterResponse(user.getId(), user.getEmail(), user.getGoal(), accessToken);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository
                .findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtService.generateToken(user.getId(), user.getEmail());
        return new LoginResponse(accessToken, jwtService.getExpirationSeconds());
    }

    @Override
    public MeResponse me(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(NoSuchElementException::new);
        return new MeResponse(user.getId(), user.getEmail(), user.getGoal());
    }
}
