package com.project.movienight.application.ports.output

import com.project.movienight.domain.model.Film
import java.util.UUID

interface FilmRepositoryPort {
    fun save(film: Film): Film

    fun findById(id: UUID): Film?

    fun findAll(): List<Film>

    fun deleteById(id: UUID)
}
