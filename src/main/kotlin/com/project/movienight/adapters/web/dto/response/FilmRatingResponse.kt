package com.project.movienight.adapters.web.dto.response

import com.project.movienight.domain.model.FilmRating
import java.time.LocalDateTime
import java.util.UUID

data class FilmRatingResponse(
    val id: UUID,
    val userId: UUID,
    val filmId: UUID,
    val score: Int,
    val note: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun fromDomain(rating: FilmRating): FilmRatingResponse =
            FilmRatingResponse(
                id = rating.id,
                userId = rating.userId,
                filmId = rating.filmId,
                score = rating.score,
                note = rating.note,
                createdAt = rating.createdAt,
                updatedAt = rating.updatedAt,
            )
    }
}
