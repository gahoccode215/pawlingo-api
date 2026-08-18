package com.pawlingo.api.auth.service.impl;

import com.pawlingo.api.auth.dto.request.GoogleAuthRequest;
import com.pawlingo.api.auth.dto.request.LoginRequest;
import com.pawlingo.api.auth.dto.request.RegisterRequest;
import com.pawlingo.api.auth.dto.response.GoogleAuthResponse;
import com.pawlingo.api.auth.dto.response.LoginResponse;
import com.pawlingo.api.auth.dto.response.MeResponse;
import com.pawlingo.api.auth.dto.response.RegisterResponse;
import com.pawlingo.api.auth.service.AuthService;
import com.pawlingo.api.auth.service.GoogleTokenVerifier;
import com.pawlingo.api.auth.service.GoogleUserInfo;
import com.pawlingo.api.auth.service.JwtService;
import com.pawlingo.api.common.exception.BusinessException;
import com.pawlingo.api.common.exception.ErrorCode;
import com.pawlingo.api.user.entity.User;
import com.pawlingo.api.user.enums.AuthProvider;
import com.pawlingo.api.user.enums.Goal;
import com.pawlingo.api.user.repository.UserRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            GoogleTokenVerifier googleTokenVerifier) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL, "Email already registered: " + email);
        }

        User user = User.builder()
                .email(email)
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
                .findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtService.generateToken(user.getId(), user.getEmail());
        return new LoginResponse(accessToken, jwtService.getExpirationSeconds());
    }

    @Override
    @Transactional
    public GoogleAuthResponse loginWithGoogle(GoogleAuthRequest request) {
        GoogleUserInfo googleUserInfo = googleTokenVerifier.verify(request.idToken());
        if (!googleUserInfo.emailVerified()) {
            throw new BusinessException(ErrorCode.GOOGLE_EMAIL_NOT_VERIFIED);
        }
        String email = normalizeEmail(googleUserInfo.email());

        Optional<User> byGoogleId = userRepository.findByGoogleId(googleUserInfo.googleId());
        if (byGoogleId.isPresent()) {
            return issueGoogleAuthResponse(byGoogleId.get(), false);
        }

        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            if (existing.getAuthProvider() == AuthProvider.LOCAL) {
                throw new BusinessException(ErrorCode.ACCOUNT_EXISTS_WITH_PASSWORD);
            }
            // GOOGLE user matched by email but missing googleId (legacy data) - backfill it.
            existing.setGoogleId(googleUserInfo.googleId());
            return issueGoogleAuthResponse(userRepository.save(existing), false);
        }

        User newUser = User.builder()
                .email(email)
                .passwordHash(null)
                .goal(Goal.BEGINNER)
                .authProvider(AuthProvider.GOOGLE)
                .googleId(googleUserInfo.googleId())
                .build();
        return issueGoogleAuthResponse(userRepository.save(newUser), true);
    }

    private GoogleAuthResponse issueGoogleAuthResponse(User user, boolean isNewUser) {
        String accessToken = jwtService.generateToken(user.getId(), user.getEmail());
        return new GoogleAuthResponse(
                user.getId(), user.getEmail(), user.getGoal(), accessToken, jwtService.getExpirationSeconds(), isNewUser);
    }

    @Override
    public MeResponse me(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(NoSuchElementException::new);
        return new MeResponse(user.getId(), user.getEmail(), user.getGoal());
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase();
    }
}
