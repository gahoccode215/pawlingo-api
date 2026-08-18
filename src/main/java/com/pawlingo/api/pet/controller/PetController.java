package com.pawlingo.api.pet.controller;

import com.pawlingo.api.common.response.ApiResponseDTO;
import com.pawlingo.api.pet.dto.response.PetResponse;
import com.pawlingo.api.pet.service.PetService;
import com.pawlingo.api.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pet")
@Tag(name = "Pet", description = "Current user's pet")
@SecurityRequirement(name = "bearerAuth")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping
    @Operation(summary = "Get the current user's pet")
    public ResponseEntity<ApiResponseDTO<PetResponse>> getMyPet(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponseDTO.ok(petService.getMyPet(currentUser.getId())));
    }
}
