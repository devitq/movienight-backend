package com.project.movienight.adapters.web.dto.response

import com.project.movienight.application.ports.input.RecommendationOnboardingResult
import java.util.UUID

data class RecommendationOnboardingResponse(
    val userId: UUID,
    val preferences: UserPreferencesResponse,
    val weights: UserRecommendationWeightsResponse,
    val likedFilmsCount: Int,
    val dislikedFilmsCount: Int,
    val libraryFilmsCount: Int,
    val watchedFilmsCount: Int,
) {
    companion object {
        fun fromApplication(result: RecommendationOnboardingResult): RecommendationOnboardingResponse =
            RecommendationOnboardingResponse(
                userId = result.userId,
                preferences = UserPreferencesResponse.fromDomain(result.preferences),
                weights = UserRecommendationWeightsResponse.fromDomain(result.weights),
                likedFilmsCount = result.likedFilmsCount,
                dislikedFilmsCount = result.dislikedFilmsCount,
                libraryFilmsCount = result.libraryFilmsCount,
                watchedFilmsCount = result.watchedFilmsCount,
            )
    }
}
