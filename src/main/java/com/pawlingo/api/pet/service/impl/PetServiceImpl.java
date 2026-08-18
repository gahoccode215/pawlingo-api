package com.pawlingo.api.pet.service.impl;

import com.pawlingo.api.common.exception.BusinessException;
import com.pawlingo.api.common.exception.ErrorCode;
import com.pawlingo.api.pet.dto.response.PetResponse;
import com.pawlingo.api.pet.entity.Pet;
import com.pawlingo.api.pet.repository.PetRepository;
import com.pawlingo.api.pet.service.PetService;
import com.pawlingo.api.pet.service.PetStagePolicy;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PetServiceImpl implements PetService {

    private static final int INITIAL_STAGE = 1;
    private static final int INITIAL_XP = 0;
    private static final int INITIAL_ENERGY = 100;
    private static final int MIN_ENERGY = 0;
    private static final int MAX_ENERGY = 100;

    private final PetRepository petRepository;
    private final PetStagePolicy petStagePolicy;

    public PetServiceImpl(PetRepository petRepository, PetStagePolicy petStagePolicy) {
        this.petRepository = petRepository;
        this.petStagePolicy = petStagePolicy;
    }

    @Override
    @Transactional
    public void createDefaultPet(UUID userId) {
        Pet pet = Pet.builder()
                .userId(userId)
                .stage(INITIAL_STAGE)
                .xp(INITIAL_XP)
                .energy(INITIAL_ENERGY)
                .build();
        petRepository.save(pet);
    }

    @Override
    public PetResponse getMyPet(UUID userId) {
        Pet pet = findByUserId(userId);
        return new PetResponse(pet.getId(), pet.getStage(), pet.getXp(), pet.getEnergy());
    }

    @Override
    @Transactional
    public Pet applyProgressResult(UUID userId, int xpDelta, int energyDelta) {
        Pet pet = findByUserId(userId);
        int newXp = Math.max(INITIAL_XP, pet.getXp() + xpDelta);
        int newEnergy = clamp(pet.getEnergy() + energyDelta, MIN_ENERGY, MAX_ENERGY);

        pet.setXp(newXp);
        pet.setEnergy(newEnergy);
        pet.setStage(petStagePolicy.resolveStage(newXp));

        return petRepository.save(pet);
    }

    private Pet findByUserId(UUID userId) {
        return petRepository.findByUserId(userId).orElseThrow(() -> new BusinessException(ErrorCode.PET_NOT_FOUND));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
