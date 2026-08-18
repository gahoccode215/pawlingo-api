package com.pawlingo.api.pet.service;

import org.springframework.stereotype.Component;

@Component
public class PetStagePolicy {

    private static final int[] STAGE_XP_THRESHOLDS = {0, 100, 300, 600, 1000};

    public int resolveStage(int xp) {
        int stage = 1;
        for (int i = 0; i < STAGE_XP_THRESHOLDS.length; i++) {
            if (xp >= STAGE_XP_THRESHOLDS[i]) {
                stage = i + 1;
            }
        }
        return stage;
    }
}
