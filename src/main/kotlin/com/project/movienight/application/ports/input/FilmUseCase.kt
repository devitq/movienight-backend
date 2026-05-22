package com.project.movienight.application.ports.input

import com.project.movienight.domain.model.ContentType
import com.project.movienight.domain.model.Film
import java.util.UUID

interface FilmUseCase {
    fun create(command: CreateFilmCommand): Film

    fun edit(
        id: UUID,
        command: EditFilmCommand,
    ): Film

    fun delete(id: UUID)

    fun getById(id: UUID): Film

    fun getAll(): List<Film>

    fun searchByTitle(title: String): Film?
}

data class CreateFilmCommand(
    val title: String,
    val description: String,
    val contentType: ContentType = ContentType.FILM,
    val releaseYear: Int? = null,
    val genres: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val directors: List<String> = emptyList(),
    val imdbRating: Double? = null,
    val platformRating: Double? = null,
    val externalUrl: String? = null,
    val jellyfinItemId: String? = null,
    val jellyfinLibraryId: String? = null,
)

data class EditFilmCommand(
    val title: String,
    val description: String,
    val contentType: ContentType = ContentType.FILM,
    val releaseYear: Int? = null,
    val genres: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val directors: List<String> = emptyList(),
    val imdbRating: Double? = null,
    val platformRating: Double? = null,
    val externalUrl: String? = null,
    val jellyfinItemId: String? = null,
    val jellyfinLibraryId: String? = null,
)
