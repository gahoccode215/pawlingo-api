package com.pawlingo.api.auth.dto.response;

import com.pawlingo.api.user.enums.Goal;
import java.util.UUID;

public record MeResponse(UUID id, String email, Goal goal) {}
