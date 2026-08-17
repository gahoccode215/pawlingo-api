package com.pawlingo.api.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "user@example.com") @NotBlank(message = "email is required")
                @Email(message = "email must be a valid address")
                String email,
        @Schema(example = "password123") @NotBlank(message = "password is required") String password) {}
