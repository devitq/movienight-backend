package com.project.movienight.application.ports.input

import com.project.movienight.domain.model.ContentType
import com.project.movienight.domain.model.RecommendationEvent
import com.project.movienight.domain.model.RecommendationResult
import java.util.UUID

interface GetRecommendationsUseCase {
    fun recommend(query: RecommendationQuery): List<RecommendationResult>
}

data class RecommendationQuery(
    val userId: UUID,
    val contentType: ContentType? = null,
    val mood: String? = null,
    val libraryOnly: Boolean = false,
    val limit: Int = 10,
)

interface AcceptRecommendationUseCase {
    fun accept(command: AcceptRecommendationCommand): RecommendationEvent
}

data class AcceptRecommendationCommand(
    val userId: UUID,
    val filmId: UUID,
)

interface RejectRecommendationUseCase {
    fun reject(command: RejectRecommendationCommand): RecommendationEvent
}

data class RejectRecommendationCommand(
    val userId: UUID,
    val filmId: UUID,
)
