package com.project.movienight.application.ports.input

import com.project.movienight.domain.model.FilmLibrary
import java.time.LocalDateTime
import java.util.UUID

interface CreateFilmLibraryUseCase {
    fun create(command: CreateFilmLibraryCommand): FilmLibrary
}

data class CreateFilmLibraryCommand(
    val userId: UUID,
    val name: String = "Мои фильмы",
)

interface AddFilmToLibraryUseCase {
    fun addFilm(command: AddFilmToLibraryCommand): FilmLibrary
}

data class AddFilmToLibraryCommand(
    val userId: UUID,
    val filmId: UUID,
)

interface MarkFilmViewedUseCase {
    fun markViewed(command: MarkFilmViewedCommand): FilmLibrary
}

data class MarkFilmViewedCommand(
    val userId: UUID,
    val filmId: UUID,
    val watchedAt: LocalDateTime? = null,
)

interface RemoveFilmFromLibraryUseCase {
    fun removeFilm(command: RemoveFilmFromLibraryCommand): FilmLibrary
}

data class RemoveFilmFromLibraryCommand(
    val userId: UUID,
    val filmId: UUID,
    val libraryId: UUID? = null,
)

interface GetFilmLibraryUseCase {
    fun getLibrary(query: GetFilmLibraryQuery): FilmLibrary
}

data class GetFilmLibraryQuery(
    val userId: UUID,
)

interface ListFilmLibraryEntriesUseCase {
    fun list(userId: UUID): List<FilmLibrary>
}
