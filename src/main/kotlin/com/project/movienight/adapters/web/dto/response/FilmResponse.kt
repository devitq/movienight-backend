package com.project.movienight.adapters.web.dto.response

import com.project.movienight.domain.model.ContentType
import com.project.movienight.domain.model.Film
import java.util.UUID

data class FilmResponse(
    val id: UUID,
    val title: String,
    val description: String,
    val contentType: ContentType,
    val releaseYear: Int?,
    val genres: List<String>,
    val cast: List<String>,
    val directors: List<String>,
    val imdbRating: Double?,
    val platformRating: Double?,
    val externalUrl: String?,
    val jellyfinItemId: String?,
    val jellyfinLibraryId: String?,
) {
    companion object {
        fun fromDomain(film: Film): FilmResponse =
            FilmResponse(
                id = film.id,
                title = film.title,
                description = film.description,
                contentType = film.contentType,
                releaseYear = film.releaseYear,
                genres = film.genres,
                cast = film.cast,
                directors = film.directors,
                imdbRating = film.imdbRating,
                platformRating = film.platformRating,
                externalUrl = film.externalUrl,
                jellyfinItemId = film.jellyfinItemId,
                jellyfinLibraryId = film.jellyfinLibraryId,
            )
    }
}
