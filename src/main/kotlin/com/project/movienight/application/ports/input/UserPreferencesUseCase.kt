package com.project.movienight.application.ports.input

import com.project.movienight.domain.model.ContentType
import com.project.movienight.domain.model.UserPreferences
import java.util.UUID

interface UserPreferencesUseCase {
    fun upsert(command: UpsertUserPreferencesCommand): UserPreferences

    fun get(userId: UUID): UserPreferences?
}

data class UpsertUserPreferencesCommand(
    val userId: UUID,
    val weightedGenres: Map<String, Int> = emptyMap(),
    val plotTypes: List<String> = emptyList(),
    val eras: List<String> = emptyList(),
    val castAndDirectors: List<String> = emptyList(),
    val moods: List<String> = emptyList(),
    val contentTypes: List<ContentType> = emptyList(),
)
