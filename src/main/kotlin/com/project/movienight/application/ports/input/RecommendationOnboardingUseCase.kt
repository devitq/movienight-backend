package com.project.movienight.application.ports.input

import com.project.movienight.domain.model.ContentType
import com.project.movienight.domain.model.RecommendationStyle
import com.project.movienight.domain.model.UserPreferences
import com.project.movienight.domain.model.UserRecommendationWeights
import java.util.UUID

interface CompleteRecommendationOnboardingUseCase {
    fun complete(command: CompleteRecommendationOnboardingCommand): RecommendationOnboardingResult
}

data class CompleteRecommendationOnboardingCommand(
    val userId: UUID,
    val weightedGenres: Map<String, Int> = emptyMap(),
    val plotTypes: List<String> = emptyList(),
    val eras: List<String> = emptyList(),
    val castAndDirectors: List<String> = emptyList(),
    val moods: List<String> = emptyList(),
    val contentTypes: List<ContentType> = emptyList(),
    val likedFilmIds: List<UUID> = emptyList(),
    val dislikedFilmIds: List<UUID> = emptyList(),
    val libraryFilmIds: List<UUID> = emptyList(),
    val watchedFilmIds: List<UUID> = emptyList(),
    val recommendationStyle: RecommendationStyle = RecommendationStyle.BALANCED,
)

data class RecommendationOnboardingResult(
    val userId: UUID,
    val preferences: UserPreferences,
    val weights: UserRecommendationWeights,
    val likedFilmsCount: Int,
    val dislikedFilmsCount: Int,
    val libraryFilmsCount: Int,
    val watchedFilmsCount: Int,
)
