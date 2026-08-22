package com.pawlingo.api.auth.controller;

import com.pawlingo.api.auth.dto.request.GoogleAuthRequest;
import com.pawlingo.api.auth.dto.request.LoginRequest;
import com.pawlingo.api.auth.dto.request.LogoutRequest;
import com.pawlingo.api.auth.dto.request.RefreshRequest;
import com.pawlingo.api.auth.dto.request.RegisterRequest;
import com.pawlingo.api.auth.dto.response.GoogleAuthResponse;
import com.pawlingo.api.auth.dto.response.LoginResponse;
import com.pawlingo.api.auth.dto.response.MeResponse;
import com.pawlingo.api.auth.dto.response.RefreshResponse;
import com.pawlingo.api.auth.dto.response.RegisterResponse;
import com.pawlingo.api.auth.service.AuthService;
import com.pawlingo.api.common.response.ApiResponseDTO;
import com.pawlingo.api.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Register, login, and current-user lookup")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new account with email + password")
    public ResponseEntity<ApiResponseDTO<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDTO.ok(response));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with email + password, returns a JWT access token")
    public ResponseEntity<ApiResponseDTO<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponseDTO.ok(response));
    }

    @PostMapping("/google")
    @Operation(summary = "Log in or register via a Google ID token, returns a JWT access token")
    public ResponseEntity<ApiResponseDTO<GoogleAuthResponse>> loginWithGoogle(
            @Valid @RequestBody GoogleAuthRequest request) {
        GoogleAuthResponse response = authService.loginWithGoogle(request);
        HttpStatus status = response.isNewUser() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponseDTO.ok(response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponseDTO<MeResponse>> me(@AuthenticationPrincipal User currentUser) {
        MeResponse response = authService.me(currentUser.getId());
        return ResponseEntity.ok(ApiResponseDTO.ok(response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new access + refresh token pair (rotates)")
    public ResponseEntity<ApiResponseDTO<RefreshResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponseDTO.ok(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke a refresh token, logging out that session")
    public ResponseEntity<ApiResponseDTO<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponseDTO.ok(null));
    }
}
