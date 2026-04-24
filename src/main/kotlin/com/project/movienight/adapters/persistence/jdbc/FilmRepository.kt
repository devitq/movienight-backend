package com.project.movienight.adapters.persistence.jdbc

import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.domain.model.Film
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

@Repository
class FilmRepository(
    private val jdbc: JdbcTemplate,
) : FilmRepositoryPort {
    private val filmRowMapper = { rs: ResultSet, _: Int ->
        Film(
            id = UUID.fromString(rs.getString("id")),
            title = rs.getString("title"),
            description = rs.getString("description"),
        )
    }

    override fun save(film: Film): Film {
        val updatedRows =
            jdbc.update(
                """
                UPDATE films
                SET title = ?, description = ?
                WHERE id = ?
                """.trimIndent(),
                film.title,
                film.description,
                film.id,
            )
        if (updatedRows == 0) {
            jdbc.update(
                """
                INSERT INTO films (id, title, description)
                VALUES (?, ?, ?)
                """.trimIndent(),
                film.id,
                film.title,
                film.description,
            )
        }
        return film
    }

    override fun findById(id: UUID): Film? {
        val films =
            jdbc.query(
                "SELECT id, title, description FROM films WHERE id = ?",
                filmRowMapper,
                id,
            )
        return films.firstOrNull()
    }

    override fun findAll(): List<Film> =
        jdbc.query(
            "SELECT id, title, description FROM films",
            filmRowMapper,
        )

    override fun findByTitle(title: String): Film? {
        val films =
            jdbc.query(
                "SELECT id, title, description FROM films WHERE title = ? ORDER BY id LIMIT 1",
                filmRowMapper,
                title,
            )
        return films.firstOrNull()
    }

    override fun deleteById(id: UUID) {
        jdbc.update("DELETE FROM films WHERE id = ?", id)
    }
}
