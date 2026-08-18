package com.pawlingo.api.progress.dto.request;

import com.pawlingo.api.vocab.enums.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RecordProgressRequest(
        @Schema(example = "11111111-1111-1111-1111-111111111111") @NotNull(message = "vocabWordId is required")
                UUID vocabWordId,
        @Schema(example = "quiz") @NotNull(message = "activityType is required") ActivityType activityType,
        @Schema(example = "true") @NotNull(message = "correct is required") Boolean correct) {}
