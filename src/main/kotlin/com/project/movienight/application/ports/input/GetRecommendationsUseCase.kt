package com.project.movienight.application.ports.input

import com.project.movienight.domain.model.ContentType
import com.project.movienight.domain.model.RecommendationResult
import java.util.UUID

interface GetRecommendationsUseCase {
    fun recommend(query: RecommendationQuery): List<RecommendationResult>
}

data class RecommendationQuery(
    val userId: UUID,
    val contentType: ContentType? = null,
    val mood: String? = null,
    val limit: Int = 10,
)
