package com.pawlingo.api.vocab.dto.request;

import jakarta.validation.constraints.NotNull;

public record FavoriteRequest(@NotNull(message = "isFavorite is required") Boolean isFavorite) {}
