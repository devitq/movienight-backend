package com.project.movienight.application.ports.input

import com.project.movienight.domain.model.FilmRating
import java.util.UUID

interface RateFilmUseCase {
    fun rate(command: RateFilmCommand): FilmRating
}

data class RateFilmCommand(
    val userId: UUID,
    val filmId: UUID,
    val score: Int,
    val note: String? = null,
)

interface GetFilmRatingsUseCase {
    fun getRatings(userId: UUID): List<FilmRating>
}
