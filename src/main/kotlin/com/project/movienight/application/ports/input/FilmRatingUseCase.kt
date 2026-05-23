package com.project.movienight.application.ports.input

import com.project.movienight.domain.model.FilmRating
import java.util.UUID

interface FilmRatingUseCase {
    fun rate(command: RateFilmCommand): FilmRating

    fun getRatings(userId: UUID): List<FilmRating>
}

data class RateFilmCommand(
    val userId: UUID,
    val filmId: UUID,
    val score: Int,
    val note: String? = null,
)
