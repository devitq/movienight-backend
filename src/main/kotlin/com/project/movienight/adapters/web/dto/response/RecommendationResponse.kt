package com.project.movienight.adapters.web.dto.response

import com.project.movienight.domain.model.RecommendationResult
import java.util.UUID

data class RecommendationResponse(
    val filmId: UUID,
    val title: String,
    val score: Double,
    val reasons: List<String>,
    val jellyfinItemId: String?,
    val watchUrl: String?,
    val film: FilmResponse,
) {
    companion object {
        fun fromDomain(
            recommendation: RecommendationResult,
            watchUrl: String?,
        ): RecommendationResponse {
            val film = recommendation.film
            return RecommendationResponse(
                filmId = film.id,
                title = film.title,
                score = recommendation.score,
                reasons = recommendation.reasons,
                jellyfinItemId = film.jellyfinItemId,
                watchUrl = watchUrl,
                film = FilmResponse.fromDomain(film),
            )
        }
    }
}
