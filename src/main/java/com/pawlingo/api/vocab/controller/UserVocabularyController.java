package com.pawlingo.api.vocab.controller;

import com.pawlingo.api.common.response.ApiResponseDTO;
import com.pawlingo.api.common.response.PageMeta;
import com.pawlingo.api.user.entity.User;
import com.pawlingo.api.vocab.dto.request.AddVocabularyRequest;
import com.pawlingo.api.vocab.dto.request.FavoriteRequest;
import com.pawlingo.api.vocab.dto.response.UserVocabularyResponse;
import com.pawlingo.api.vocab.enums.VocabularyStatus;
import com.pawlingo.api.vocab.service.AddVocabularyResult;
import com.pawlingo.api.vocab.service.UserVocabularyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/vocabularies")
@Tag(name = "User Vocabulary", description = "The current user's saved words - save, remove, favorite, list")
@SecurityRequirement(name = "bearerAuth")
public class UserVocabularyController {

    private final UserVocabularyService userVocabularyService;

    public UserVocabularyController(UserVocabularyService userVocabularyService) {
        this.userVocabularyService = userVocabularyService;
    }

    @PostMapping
    @Operation(summary = "Save a word to the current user's vocabulary (idempotent)")
    public ResponseEntity<ApiResponseDTO<UserVocabularyResponse>> addVocabulary(
            @AuthenticationPrincipal User currentUser, @Valid @RequestBody AddVocabularyRequest request) {
        AddVocabularyResult result = userVocabularyService.addVocabulary(currentUser.getId(), request.wordId());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponseDTO.ok(result.response()));
    }

    @DeleteMapping("/{wordId}")
    @Operation(summary = "Remove a word from the current user's vocabulary")
    public ResponseEntity<Void> removeVocabulary(@AuthenticationPrincipal User currentUser, @PathVariable UUID wordId) {
        userVocabularyService.removeVocabulary(currentUser.getId(), wordId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{wordId}/favorite")
    @Operation(summary = "Favorite/unfavorite a word (implicitly saves it if not already saved)")
    public ResponseEntity<ApiResponseDTO<UserVocabularyResponse>> setFavorite(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID wordId,
            @Valid @RequestBody FavoriteRequest request) {
        UserVocabularyResponse response =
                userVocabularyService.setFavorite(currentUser.getId(), wordId, request.isFavorite());
        return ResponseEntity.ok(ApiResponseDTO.ok(response));
    }

    @GetMapping
    @Operation(summary = "List the current user's saved words")
    public ResponseEntity<ApiResponseDTO<List<UserVocabularyResponse>>> listUserVocabularies(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Boolean isFavorite,
            @RequestParam(required = false) VocabularyStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<UserVocabularyResponse> page =
                userVocabularyService.listUserVocabularies(currentUser.getId(), isFavorite, status, pageable);
        return ResponseEntity.ok(ApiResponseDTO.ok(page.getContent(), PageMeta.of(page)));
    }
}
