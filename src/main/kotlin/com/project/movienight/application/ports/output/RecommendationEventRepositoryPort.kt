package com.project.movienight.application.ports.output

import com.project.movienight.domain.model.RecommendationEvent
import java.util.UUID

interface RecommendationEventRepositoryPort {
    fun save(event: RecommendationEvent): RecommendationEvent

    fun findByUserId(userId: UUID): List<RecommendationEvent>

    fun findLatestRecommended(
        userId: UUID,
        filmId: UUID,
    ): RecommendationEvent?
}
