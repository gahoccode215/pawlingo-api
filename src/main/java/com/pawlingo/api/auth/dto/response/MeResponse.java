package com.pawlingo.api.auth.dto.response;

import com.pawlingo.api.user.enums.AuthProvider;
import com.pawlingo.api.user.enums.Goal;
import java.time.Instant;
import java.util.UUID;

public record MeResponse(UUID id, String email, Goal goal, AuthProvider authProvider, Instant createdAt) {}
