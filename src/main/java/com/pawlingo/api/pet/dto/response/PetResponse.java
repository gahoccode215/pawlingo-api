package com.pawlingo.api.pet.dto.response;

import java.util.UUID;

public record PetResponse(UUID id, int stage, int xp, int energy) {}
