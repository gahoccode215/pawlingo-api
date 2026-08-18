package com.pawlingo.api.pet.service;

import com.pawlingo.api.pet.dto.response.PetResponse;
import com.pawlingo.api.pet.entity.Pet;
import java.util.UUID;

public interface PetService {

    void createDefaultPet(UUID userId);

    PetResponse getMyPet(UUID userId);

    Pet applyProgressResult(UUID userId, int xpDelta, int energyDelta);
}
