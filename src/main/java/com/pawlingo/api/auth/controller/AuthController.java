package com.pawlingo.api.auth.controller;

import com.pawlingo.api.auth.dto.request.LoginRequest;
import com.pawlingo.api.auth.dto.request.RegisterRequest;
import com.pawlingo.api.auth.dto.response.LoginResponse;
import com.pawlingo.api.auth.dto.response.MeResponse;
import com.pawlingo.api.auth.dto.response.RegisterResponse;
import com.pawlingo.api.auth.service.AuthService;
import com.pawlingo.api.common.response.ApiResponseDTO;
import com.pawlingo.api.user.User;
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
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDTO.ok(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponseDTO.ok(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseDTO<MeResponse>> me(@AuthenticationPrincipal User currentUser) {
        MeResponse response = authService.me(currentUser.getId());
        return ResponseEntity.ok(ApiResponseDTO.ok(response));
    }
}
