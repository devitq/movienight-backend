package com.project.movienight.adapters.web.dto.response

import com.project.movienight.domain.model.FilmLibrary
import java.util.UUID

data class FilmLibraryResponse(
    val id: UUID,
    val userId: UUID,
    val filmId: UUID,
    val comment: String?,
    val isViewed: Boolean,
) {
    companion object {
        fun fromDomain(filmLibrary: FilmLibrary): FilmLibraryResponse =
            FilmLibraryResponse(
                id = filmLibrary.id,
                userId = filmLibrary.userId,
                filmId = filmLibrary.filmId,
                comment = filmLibrary.comment,
                isViewed = filmLibrary.isViewed,
            )
    }
}
