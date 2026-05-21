package com.project.movienight.application.ports.input

import com.project.movienight.domain.model.UserRecommendationWeights
import java.util.UUID

interface GetUserRecommendationWeightsUseCase {
    fun get(userId: UUID): UserRecommendationWeights
}

interface UpdateUserRecommendationWeightsUseCase {
    fun update(command: UpdateUserRecommendationWeightsCommand): UserRecommendationWeights
}

data class UpdateUserRecommendationWeightsCommand(
    val userId: UUID,
    val relevanceWeight: Double,
    val qualityWeight: Double,
    val contextWeight: Double,
    val noveltyWeight: Double,
    val diversityWeight: Double,
    val genreVectorWeight: Double,
    val plotVectorWeight: Double,
    val moodVectorWeight: Double,
    val eraVectorWeight: Double,
    val peopleVectorWeight: Double,
    val contentTypeVectorWeight: Double,
)
