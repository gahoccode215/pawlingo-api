package com.pawlingo.api.progress.service;

import com.pawlingo.api.progress.dto.request.RecordProgressRequest;
import com.pawlingo.api.progress.dto.response.ProgressResultResponse;
import java.util.UUID;

public interface ProgressService {

    ProgressResultResponse recordProgress(UUID userId, RecordProgressRequest request);
}
