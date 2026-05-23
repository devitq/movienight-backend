package com.project.movienight.adapters.web.dto.request

data class UpsertUserPreferencesRequest(
    val weightedGenres: Map<String, Int> = emptyMap(),
    val plotTypes: List<String> = emptyList(),
    val eras: List<String> = emptyList(),
    val castAndDirectors: List<String> = emptyList(),
    val moods: List<String> = emptyList(),
    val contentTypes: List<String> = emptyList(),
)
