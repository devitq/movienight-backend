package com.project.movienight.application.ports.output

import com.project.movienight.domain.model.UserRecommendationWeights
import java.util.UUID

interface UserRecommendationWeightsRepositoryPort {
    fun findByUserId(userId: UUID): UserRecommendationWeights?

    fun save(weights: UserRecommendationWeights): UserRecommendationWeights
}
