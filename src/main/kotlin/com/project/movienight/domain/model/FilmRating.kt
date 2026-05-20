package com.project.movienight.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class FilmRating(
    val id: UUID,
    val userId: UUID,
    val filmId: UUID,
    val score: Int,
    val note: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = createdAt,
)
