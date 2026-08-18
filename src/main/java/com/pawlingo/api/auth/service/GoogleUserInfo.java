package com.pawlingo.api.auth.service;

public record GoogleUserInfo(String googleId, String email, boolean emailVerified) {}
