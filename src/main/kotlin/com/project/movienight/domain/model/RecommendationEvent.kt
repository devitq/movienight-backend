package com.project.movienight.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class RecommendationEvent(
    val id: UUID,
    val userId: UUID,
    val filmId: UUID,
    val eventType: RecommendationEventType,
    val score: Double? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)

enum class RecommendationEventType {
    RECOMMENDED,
    ACCEPTED,
    REJECTED,
}
