package com.project.movienight.application.ports.input

import com.project.movienight.domain.model.ContentType
import com.project.movienight.domain.model.Film
import java.util.UUID

interface CreateFilmUseCase {
    fun create(command: CreateFilmCommand): Film
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

interface EditFilmUseCase {
    fun edit(
        id: UUID,
        command: EditFilmCommand,
    ): Film
}

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

interface DeleteFilmUseCase {
    fun delete(id: UUID)
}

interface GetFilmByIdUseCase {
    fun getById(id: UUID): Film
}

interface GetAllFilmsUseCase {
    fun getAll(): List<Film>
}

interface SearchFilmByTitleUseCase {
    fun searchByTitle(title: String): Film?
}
