package com.pawlingo.api.vocab.controller;

import com.pawlingo.api.common.response.ApiResponseDTO;
import com.pawlingo.api.common.response.PageMeta;
import com.pawlingo.api.vocab.dto.response.WordDetailResponse;
import com.pawlingo.api.vocab.dto.response.WordSummaryResponse;
import com.pawlingo.api.vocab.enums.DifficultyLevel;
import com.pawlingo.api.vocab.enums.PartOfSpeech;
import com.pawlingo.api.vocab.service.VocabularyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vocabularies")
@Tag(name = "Vocabulary", description = "Browse, search, and view vocabulary words (public)")
public class VocabularyController {

    private final VocabularyService vocabularyService;

    public VocabularyController(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    @GetMapping
    @Operation(summary = "List/search/filter vocabulary words")
    public ResponseEntity<ApiResponseDTO<List<WordSummaryResponse>>> listWords(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) DifficultyLevel difficultyLevel,
            @RequestParam(required = false) PartOfSpeech partOfSpeech,
            @PageableDefault(size = 20, sort = "word") Pageable pageable) {
        Page<WordSummaryResponse> page = vocabularyService.listWords(q, difficultyLevel, partOfSpeech, pageable);
        return ResponseEntity.ok(ApiResponseDTO.ok(page.getContent(), PageMeta.of(page)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full detail for a word, including examples")
    public ResponseEntity<ApiResponseDTO<WordDetailResponse>> getWordDetail(@PathVariable UUID id) {
        WordDetailResponse response = vocabularyService.getWordDetail(id);
        return ResponseEntity.ok(ApiResponseDTO.ok(response));
    }
}
