package com.pawlingo.api.pet.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pawlingo.api.common.exception.BusinessException;
import com.pawlingo.api.common.exception.ErrorCode;
import com.pawlingo.api.pet.dto.response.PetResponse;
import com.pawlingo.api.pet.entity.Pet;
import com.pawlingo.api.pet.repository.PetRepository;
import com.pawlingo.api.pet.service.PetStagePolicy;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PetServiceImplTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private PetStagePolicy petStagePolicy;

    private PetServiceImpl petService;

    @BeforeEach
    void setUp() {
        petService = new PetServiceImpl(petRepository, petStagePolicy);
    }

    @Test
    void createDefaultPet_savesPetWithInitialValues() {
        UUID userId = UUID.randomUUID();

        petService.createDefaultPet(userId);

        verify(petRepository)
                .save(argThat(pet -> pet.getUserId().equals(userId)
                        && pet.getStage() == 1
                        && pet.getXp() == 0
                        && pet.getEnergy() == 100));
    }

    @Test
    void getMyPet_existingPet_returnsResponse() {
        UUID userId = UUID.randomUUID();
        Pet pet = Pet.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .stage(2)
                .xp(150)
                .energy(80)
                .build();
        when(petRepository.findByUserId(userId)).thenReturn(Optional.of(pet));

        PetResponse response = petService.getMyPet(userId);

        assertThat(response.stage()).isEqualTo(2);
        assertThat(response.xp()).isEqualTo(150);
        assertThat(response.energy()).isEqualTo(80);
    }

    @Test
    void getMyPet_noPet_throwsBusinessExceptionWithPetNotFoundCode() {
        UUID userId = UUID.randomUUID();
        when(petRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> petService.getMyPet(userId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PET_NOT_FOUND);
    }

    @Test
    void applyProgressResult_correctAnswer_increasesXpAndEnergyAndRecalculatesStage() {
        UUID userId = UUID.randomUUID();
        Pet pet = Pet.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .stage(1)
                .xp(95)
                .energy(90)
                .build();
        when(petRepository.findByUserId(userId)).thenReturn(Optional.of(pet));
        when(petStagePolicy.resolveStage(105)).thenReturn(2);
        when(petRepository.save(any(Pet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pet result = petService.applyProgressResult(userId, 10, 5);

        assertThat(result.getXp()).isEqualTo(105);
        assertThat(result.getEnergy()).isEqualTo(95);
        assertThat(result.getStage()).isEqualTo(2);
    }

    @Test
    void applyProgressResult_energyClampedToRange0to100() {
        UUID userId = UUID.randomUUID();
        Pet pet = Pet.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .stage(1)
                .xp(0)
                .energy(3)
                .build();
        when(petRepository.findByUserId(userId)).thenReturn(Optional.of(pet));
        when(petStagePolicy.resolveStage(0)).thenReturn(1);
        when(petRepository.save(any(Pet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pet result = petService.applyProgressResult(userId, 0, -10);

        assertThat(result.getEnergy()).isEqualTo(0);
    }
}
