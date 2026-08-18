package com.pawlingo.api.vocab.controller;

import com.pawlingo.api.common.response.ApiResponseDTO;
import com.pawlingo.api.vocab.dto.response.TopicDetailResponse;
import com.pawlingo.api.vocab.dto.response.TopicSummaryResponse;
import com.pawlingo.api.vocab.service.VocabService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vocab")
@Tag(name = "Vocabulary", description = "Vocabulary topics and words")
@SecurityRequirement(name = "bearerAuth")
public class VocabController {

    private final VocabService vocabService;

    public VocabController(VocabService vocabService) {
        this.vocabService = vocabService;
    }

    @GetMapping("/topics")
    @Operation(summary = "List all vocabulary topics")
    public ResponseEntity<ApiResponseDTO<List<TopicSummaryResponse>>> listTopics() {
        return ResponseEntity.ok(ApiResponseDTO.ok(vocabService.listTopics()));
    }

    @GetMapping("/topics/{topicCode}")
    @Operation(summary = "Get a topic's detail and its words")
    public ResponseEntity<ApiResponseDTO<TopicDetailResponse>> getTopicDetail(@PathVariable String topicCode) {
        return ResponseEntity.ok(ApiResponseDTO.ok(vocabService.getTopicDetail(topicCode)));
    }
}
