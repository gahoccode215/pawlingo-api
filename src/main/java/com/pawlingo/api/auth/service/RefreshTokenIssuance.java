package com.pawlingo.api.auth.service;

public record RefreshTokenIssuance(String token, long expiresInSeconds) {}
