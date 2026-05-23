package com.project.movienight.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class FilmLibraryEntry(
    val id: UUID,
    val userId: UUID,
    val filmId: UUID,
    val comment: String?,
    val isViewed: Boolean,
    val watchedAt: LocalDateTime? = null,
)
