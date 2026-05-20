package com.project.movienight.adapters.web.dto.response

import com.project.movienight.domain.model.ContentType
import com.project.movienight.domain.model.UserPreferences
import java.util.UUID

data class UserPreferencesResponse(
    val userId: UUID,
    val weightedGenres: Map<String, Int>,
    val plotTypes: List<String>,
    val eras: List<String>,
    val castAndDirectors: List<String>,
    val moods: List<String>,
    val contentTypes: List<ContentType>,
) {
    companion object {
        fun fromDomain(preferences: UserPreferences): UserPreferencesResponse =
            UserPreferencesResponse(
                userId = preferences.userId,
                weightedGenres = preferences.weightedGenres,
                plotTypes = preferences.plotTypes,
                eras = preferences.eras,
                castAndDirectors = preferences.castAndDirectors,
                moods = preferences.moods,
                contentTypes = preferences.contentTypes,
            )
    }
}
