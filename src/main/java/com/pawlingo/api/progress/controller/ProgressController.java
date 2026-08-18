package com.pawlingo.api.progress.controller;

import com.pawlingo.api.common.response.ApiResponseDTO;
import com.pawlingo.api.progress.dto.request.RecordProgressRequest;
import com.pawlingo.api.progress.dto.response.ProgressResultResponse;
import com.pawlingo.api.progress.service.ProgressService;
import com.pawlingo.api.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/progress")
@Tag(name = "Progress", description = "Record vocabulary learning results")
@SecurityRequirement(name = "bearerAuth")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @PostMapping
    @Operation(summary = "Record one vocabulary answer attempt, updates the user's pet XP/energy")
    public ResponseEntity<ApiResponseDTO<ProgressResultResponse>> recordProgress(
            @AuthenticationPrincipal User currentUser, @Valid @RequestBody RecordProgressRequest request) {
        return ResponseEntity.ok(ApiResponseDTO.ok(progressService.recordProgress(currentUser.getId(), request)));
    }
}
