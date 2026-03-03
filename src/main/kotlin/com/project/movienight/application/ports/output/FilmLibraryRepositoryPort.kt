package com.project.movienight.application.ports.output

import com.project.movienight.domain.model.FilmLibrary
import java.util.UUID

interface FilmLibraryRepositoryPort {
    fun save(filmLibrary: FilmLibrary): FilmLibrary

    fun findById(id: UUID): FilmLibrary?

    fun findAll(): List<FilmLibrary>

    fun deleteById(id: UUID)
}
