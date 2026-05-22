package com.project.movienight.adapters.web.dto.response

import com.project.movienight.domain.model.FilmLibraryEntry
import java.time.LocalDateTime
import java.util.UUID

data class FilmLibraryEntryResponse(
    val id: UUID,
    val userId: UUID,
    val filmId: UUID,
    val comment: String?,
    val isViewed: Boolean,
    val watchedAt: LocalDateTime?,
) {
    companion object {
        fun fromDomain(entry: FilmLibraryEntry): FilmLibraryEntryResponse =
            FilmLibraryEntryResponse(
                id = entry.id,
                userId = entry.userId,
                filmId = entry.filmId,
                comment = entry.comment,
                isViewed = entry.isViewed,
                watchedAt = entry.watchedAt,
            )
    }
}
