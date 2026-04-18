package com.project.movienight.adapters.web.dto.response

import com.project.movienight.domain.model.Film
import java.util.UUID

data class FilmResponse(
    val id: UUID,
    val title: String,
    val description: String,
) {
    companion object {
        fun fromDomain(film: Film): FilmResponse =
            FilmResponse(
                id = film.id,
                title = film.title,
                description = film.description,
            )
    }
}
