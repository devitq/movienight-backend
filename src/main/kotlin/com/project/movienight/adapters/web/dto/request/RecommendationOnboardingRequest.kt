package com.project.movienight.adapters.web.dto.request

import java.util.UUID

data class RecommendationOnboardingRequest(
    val weightedGenres: Map<String, Int> = emptyMap(),
    val plotTypes: List<String> = emptyList(),
    val eras: List<String> = emptyList(),
    val castAndDirectors: List<String> = emptyList(),
    val moods: List<String> = emptyList(),
    val contentTypes: List<String> = emptyList(),
    val likedFilmIds: List<UUID> = emptyList(),
    val dislikedFilmIds: List<UUID> = emptyList(),
    val libraryFilmIds: List<UUID> = emptyList(),
    val watchedFilmIds: List<UUID> = emptyList(),
    val recommendationStyle: String = "BALANCED",
)
