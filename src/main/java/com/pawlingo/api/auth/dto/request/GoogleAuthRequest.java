package com.pawlingo.api.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(
        @Schema(example = "eyJhbGciOi...") @NotBlank(message = "idToken is required") String idToken) {}
