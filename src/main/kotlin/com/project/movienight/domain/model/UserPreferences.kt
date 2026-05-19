package com.project.movienight.domain.model

import java.util.UUID

data class UserPreferences(
    val userId: UUID,
    val weightedGenres: Map<String, Int> = emptyMap(),
    val plotTypes: List<String> = emptyList(),
    val eras: List<String> = emptyList(),
    val castAndDirectors: List<String> = emptyList(),
    val moods: List<String> = emptyList(),
    val contentTypes: List<ContentType> = emptyList(),
)
