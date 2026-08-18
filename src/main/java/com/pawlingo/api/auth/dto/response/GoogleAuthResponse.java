package com.pawlingo.api.auth.dto.response;

import com.pawlingo.api.user.enums.Goal;
import java.util.UUID;

public record GoogleAuthResponse(
        UUID id, String email, Goal goal, String accessToken, long expiresIn, boolean isNewUser) {}
