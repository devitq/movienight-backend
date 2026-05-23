package com.project.movienight.application.ports.input

import com.project.movienight.domain.model.Film
import com.project.movienight.domain.model.FilmLibraryEntry
import java.time.LocalDateTime
import java.util.UUID

interface FilmLibraryUseCase {
    fun addFilm(command: AddFilmToLibraryCommand): FilmLibraryEntry

    fun markViewed(command: MarkFilmViewedCommand): FilmLibraryEntry

    fun removeFilm(command: RemoveFilmFromLibraryCommand): FilmLibraryEntry

    fun list(userId: UUID): List<FilmLibraryEntry>

    fun listAvailableFilms(userId: UUID): List<Film>
}

data class AddFilmToLibraryCommand(
    val userId: UUID,
    val filmId: UUID,
)

data class MarkFilmViewedCommand(
    val userId: UUID,
    val filmId: UUID,
    val watchedAt: LocalDateTime? = null,
)

data class RemoveFilmFromLibraryCommand(
    val userId: UUID,
    val filmId: UUID,
    val entryId: UUID? = null,
)
