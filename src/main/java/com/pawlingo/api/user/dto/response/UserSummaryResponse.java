package com.pawlingo.api.user.dto.response;

import com.pawlingo.api.user.enums.AuthProvider;
import com.pawlingo.api.user.enums.Goal;
import java.time.Instant;
import java.util.UUID;

public record UserSummaryResponse(
        UUID id, String email, Goal goal, AuthProvider authProvider, String googleId, Instant createdAt) {}
