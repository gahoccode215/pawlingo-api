package com.pawlingo.api.progress.service;

import com.pawlingo.api.vocab.enums.ActivityType;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ActivityScoringPolicy {

    private static final Map<ActivityType, ScoringRule> RULES =
            Map.of(ActivityType.QUIZ, new ScoringRule(10, 0, 5, -5));

    public ScoringRule getRule(ActivityType activityType) {
        ScoringRule rule = RULES.get(activityType);
        if (rule == null) {
            throw new IllegalStateException("No scoring rule configured for activity type: " + activityType);
        }
        return rule;
    }
}
