package com.pawlingo.api.auth.dto.response;

public record RefreshResponse(String accessToken, String refreshToken, long expiresIn) {}
