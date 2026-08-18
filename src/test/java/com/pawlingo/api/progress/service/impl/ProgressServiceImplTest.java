package com.pawlingo.api.progress.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.pawlingo.api.common.exception.BusinessException;
import com.pawlingo.api.common.exception.ErrorCode;
import com.pawlingo.api.pet.entity.Pet;
import com.pawlingo.api.pet.service.PetService;
import com.pawlingo.api.progress.dto.request.RecordProgressRequest;
import com.pawlingo.api.progress.dto.response.ProgressResultResponse;
import com.pawlingo.api.progress.repository.ProgressRepository;
import com.pawlingo.api.progress.service.ActivityScoringPolicy;
import com.pawlingo.api.vocab.enums.ActivityType;
import com.pawlingo.api.vocab.repository.VocabWordRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgressServiceImplTest {

    @Mock
    private ProgressRepository progressRepository;

    @Mock
    private VocabWordRepository vocabWordRepository;

    @Mock
    private PetService petService;

    private final ActivityScoringPolicy activityScoringPolicy = new ActivityScoringPolicy();

    private ProgressServiceImpl progressService;

    @BeforeEach
    void setUp() {
        progressService =
                new ProgressServiceImpl(progressRepository, vocabWordRepository, petService, activityScoringPolicy);
    }

    @Test
    void recordProgress_correctAnswer_earnsPositiveXpAndUpdatesPet() {
        UUID userId = UUID.randomUUID();
        UUID vocabWordId = UUID.randomUUID();
        RecordProgressRequest request = new RecordProgressRequest(vocabWordId, ActivityType.QUIZ, true);
        when(vocabWordRepository.existsById(vocabWordId)).thenReturn(true);
        Pet updatedPet = Pet.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .stage(1)
                .xp(50)
                .energy(90)
                .build();
        when(petService.applyProgressResult(eq(userId), eq(10), eq(5))).thenReturn(updatedPet);

        ProgressResultResponse response = progressService.recordProgress(userId, request);

        assertThat(response.xpEarned()).isEqualTo(10);
        assertThat(response.energyDelta()).isEqualTo(5);
        assertThat(response.totalXp()).isEqualTo(50);
        assertThat(response.petStage()).isEqualTo(1);
        assertThat(response.newEnergy()).isEqualTo(90);
    }

    @Test
    void recordProgress_wrongAnswer_earnsZeroXpAndLosesEnergy() {
        UUID userId = UUID.randomUUID();
        UUID vocabWordId = UUID.randomUUID();
        RecordProgressRequest request = new RecordProgressRequest(vocabWordId, ActivityType.QUIZ, false);
        when(vocabWordRepository.existsById(vocabWordId)).thenReturn(true);
        Pet updatedPet = Pet.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .stage(1)
                .xp(40)
                .energy(85)
                .build();
        when(petService.applyProgressResult(eq(userId), eq(0), eq(-5))).thenReturn(updatedPet);

        ProgressResultResponse response = progressService.recordProgress(userId, request);

        assertThat(response.xpEarned()).isEqualTo(0);
        assertThat(response.energyDelta()).isEqualTo(-5);
    }

    @Test
    void recordProgress_unknownVocabWord_throwsBusinessExceptionWithVocabWordNotFoundCode() {
        UUID userId = UUID.randomUUID();
        UUID vocabWordId = UUID.randomUUID();
        RecordProgressRequest request = new RecordProgressRequest(vocabWordId, ActivityType.QUIZ, true);
        when(vocabWordRepository.existsById(vocabWordId)).thenReturn(false);

        assertThatThrownBy(() -> progressService.recordProgress(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VOCAB_WORD_NOT_FOUND);
    }
}
