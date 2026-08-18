package com.pawlingo.api.progress.service.impl;

import com.pawlingo.api.common.exception.BusinessException;
import com.pawlingo.api.common.exception.ErrorCode;
import com.pawlingo.api.pet.entity.Pet;
import com.pawlingo.api.pet.service.PetService;
import com.pawlingo.api.progress.dto.request.RecordProgressRequest;
import com.pawlingo.api.progress.dto.response.ProgressResultResponse;
import com.pawlingo.api.progress.entity.Progress;
import com.pawlingo.api.progress.repository.ProgressRepository;
import com.pawlingo.api.progress.service.ActivityScoringPolicy;
import com.pawlingo.api.progress.service.ProgressService;
import com.pawlingo.api.progress.service.ScoringRule;
import com.pawlingo.api.vocab.repository.VocabWordRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgressServiceImpl implements ProgressService {

    private final ProgressRepository progressRepository;
    private final VocabWordRepository vocabWordRepository;
    private final PetService petService;
    private final ActivityScoringPolicy activityScoringPolicy;

    public ProgressServiceImpl(
            ProgressRepository progressRepository,
            VocabWordRepository vocabWordRepository,
            PetService petService,
            ActivityScoringPolicy activityScoringPolicy) {
        this.progressRepository = progressRepository;
        this.vocabWordRepository = vocabWordRepository;
        this.petService = petService;
        this.activityScoringPolicy = activityScoringPolicy;
    }

    @Override
    @Transactional
    public ProgressResultResponse recordProgress(UUID userId, RecordProgressRequest request) {
        if (!vocabWordRepository.existsById(request.vocabWordId())) {
            throw new BusinessException(ErrorCode.VOCAB_WORD_NOT_FOUND);
        }

        ScoringRule rule = activityScoringPolicy.getRule(request.activityType());
        boolean correct = request.correct();
        int xpEarned = correct ? rule.xpCorrect() : rule.xpWrong();
        int energyDelta = correct ? rule.energyCorrect() : rule.energyWrong();

        Progress progress = Progress.builder()
                .userId(userId)
                .vocabWordId(request.vocabWordId())
                .activityType(request.activityType())
                .correct(correct)
                .xpEarned(xpEarned)
                .build();
        progressRepository.save(progress);

        Pet pet = petService.applyProgressResult(userId, xpEarned, energyDelta);

        return new ProgressResultResponse(xpEarned, pet.getXp(), pet.getStage(), energyDelta, pet.getEnergy());
    }
}
