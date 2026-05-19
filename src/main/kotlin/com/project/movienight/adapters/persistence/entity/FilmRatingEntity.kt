package com.project.movienight.adapters.persistence.entity

import com.project.movienight.domain.model.FilmRating
import java.time.LocalDateTime
import java.util.UUID

data class FilmRatingEntity(
    val id: UUID,
    val userId: UUID,
    val filmId: UUID,
    val score: Int,
    val note: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

fun FilmRatingEntity.toDomain(): FilmRating =
    FilmRating(
        id = id,
        userId = userId,
        filmId = filmId,
        score = score,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun FilmRating.toEntity(): FilmRatingEntity =
    FilmRatingEntity(
        id = id,
        userId = userId,
        filmId = filmId,
        score = score,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
