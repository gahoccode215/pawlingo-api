package com.pawlingo.api.user.controller;

import com.pawlingo.api.common.response.ApiResponseDTO;
import com.pawlingo.api.user.dto.response.UserSummaryResponse;
import com.pawlingo.api.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Debug/testing endpoints for inspecting user data")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List all users (debug endpoint, no pagination)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponseDTO<List<UserSummaryResponse>>> listUsers() {
        return ResponseEntity.ok(ApiResponseDTO.ok(userService.listUsers()));
    }
}
