package com.project.movienight.domain.model

import java.util.UUID

data class RecommendationContext(
    val userId: UUID,
    val contentType: ContentType? = null,
    val mood: String? = null,
    val limit: Int = 10,
)

data class RecommendationResult(
    val film: Film,
    val score: Double,
    val reasons: List<String>,
)
