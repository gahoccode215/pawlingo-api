package com.pawlingo.api.auth.service;

import com.pawlingo.api.auth.dto.request.LoginRequest;
import com.pawlingo.api.auth.dto.request.RegisterRequest;
import com.pawlingo.api.auth.dto.response.LoginResponse;
import com.pawlingo.api.auth.dto.response.MeResponse;
import com.pawlingo.api.auth.dto.response.RegisterResponse;
import java.util.UUID;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    MeResponse me(UUID userId);
}
