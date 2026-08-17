package com.pawlingo.api.auth.dto.response;

public record LoginResponse(String accessToken, long expiresIn) {}
