package com.project.movienight.application.ports.output

import com.project.movienight.domain.model.FilmLibraryEntry
import java.util.UUID

interface FilmLibraryEntryRepositoryPort {
    fun save(entry: FilmLibraryEntry): FilmLibraryEntry

    fun findById(id: UUID): FilmLibraryEntry?

    fun findByUserId(userId: UUID): List<FilmLibraryEntry>

    fun findByUserIdAndFilmId(
        userId: UUID,
        filmId: UUID,
    ): FilmLibraryEntry?

    fun findAll(): List<FilmLibraryEntry>

    fun deleteById(id: UUID)
}
