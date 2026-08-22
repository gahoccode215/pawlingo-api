package com.pawlingo.api.auth.service;

import java.util.UUID;

public record RotatedRefreshToken(UUID userId, RefreshTokenIssuance issuance) {}
