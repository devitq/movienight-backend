package com.project.movienight.application.ports.output

import com.project.movienight.domain.model.FilmRating
import java.util.UUID

interface FilmRatingRepositoryPort {
    fun save(rating: FilmRating): FilmRating

    fun findByUserId(userId: UUID): List<FilmRating>

    fun findByUserIdAndFilmId(
        userId: UUID,
        filmId: UUID,
    ): FilmRating?
}
