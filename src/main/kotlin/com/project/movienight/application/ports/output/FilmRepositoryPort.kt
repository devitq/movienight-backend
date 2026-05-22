package com.project.movienight.application.ports.output

import com.project.movienight.domain.model.Film
import java.util.UUID

interface FilmRepositoryPort {
    fun save(film: Film): Film

    fun findById(id: UUID): Film?

    fun findByJellyfinItemId(jellyfinItemId: String): Film?

    fun findByJellyfinLibraryId(jellyfinLibraryId: String): Film?

    fun findAll(): List<Film>

    fun findByTitle(title: String): Film?

    fun deleteById(id: UUID)
}
