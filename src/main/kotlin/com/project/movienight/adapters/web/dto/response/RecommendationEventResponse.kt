package com.project.movienight.adapters.web.dto.response

import com.project.movienight.domain.model.RecommendationEvent
import com.project.movienight.domain.model.RecommendationEventType
import java.time.LocalDateTime
import java.util.UUID

data class RecommendationEventResponse(
    val id: UUID,
    val userId: UUID,
    val filmId: UUID,
    val eventType: RecommendationEventType,
    val score: Double?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun fromDomain(event: RecommendationEvent): RecommendationEventResponse =
            RecommendationEventResponse(
                id = event.id,
                userId = event.userId,
                filmId = event.filmId,
                eventType = event.eventType,
                score = event.score,
                createdAt = event.createdAt,
            )
    }
}
