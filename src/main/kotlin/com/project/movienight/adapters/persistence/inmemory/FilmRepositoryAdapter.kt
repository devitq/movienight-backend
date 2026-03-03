package com.project.movienight.adapters.persistence.inmemory

import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.domain.model.Film
import org.springframework.stereotype.Repository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Repository
class FilmRepositoryAdapter : FilmRepositoryPort {
    private val films: MutableMap<UUID, Film> = ConcurrentHashMap()

    override fun save(film: Film): Film {
        films[film.id] = film

        return film
    }

    override fun findById(id: UUID): Film? = films[id]

    override fun findAll(): List<Film> = films.values.toList()

    override fun deleteById(id: UUID) {
        films.remove(id)
    }
}
