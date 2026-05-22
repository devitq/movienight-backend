package com.project.movienight.adapters.web.dto.response

import com.project.movienight.domain.model.UserRecommendationWeights
import java.time.LocalDateTime
import java.util.UUID

data class UserRecommendationWeightsResponse(
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
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun fromDomain(weights: UserRecommendationWeights): UserRecommendationWeightsResponse =
            UserRecommendationWeightsResponse(
                userId = weights.userId,
                relevanceWeight = weights.relevanceWeight,
                qualityWeight = weights.qualityWeight,
                contextWeight = weights.contextWeight,
                noveltyWeight = weights.noveltyWeight,
                diversityWeight = weights.diversityWeight,
                genreVectorWeight = weights.genreVectorWeight,
                plotVectorWeight = weights.plotVectorWeight,
                moodVectorWeight = weights.moodVectorWeight,
                eraVectorWeight = weights.eraVectorWeight,
                peopleVectorWeight = weights.peopleVectorWeight,
                contentTypeVectorWeight = weights.contentTypeVectorWeight,
                updatedAt = weights.updatedAt,
            )
    }
}
