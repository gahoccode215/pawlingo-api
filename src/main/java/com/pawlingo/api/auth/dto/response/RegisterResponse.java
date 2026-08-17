package com.pawlingo.api.auth.dto.response;

import com.pawlingo.api.user.enums.Goal;
import java.util.UUID;

public record RegisterResponse(UUID id, String email, Goal goal, String accessToken) {}
