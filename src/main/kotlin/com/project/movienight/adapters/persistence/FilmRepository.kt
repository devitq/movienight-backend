package com.project.movienight.adapters.persistence

import com.project.movienight.domain.model.Film
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class FilmRepository(private val jdbc: JdbcTemplate) {

    fun findById(id: UUID): Film? =
        jdbc.queryForObject(
            "SELECT * FROM films WHERE id = ?",
            { rs, _ ->
                Film(
                    id = UUID.fromString(rs.getString("id")),
                    title = rs.getString("title"),
                    description = rs.getString("description"),
                )
            },
            id.toString()
        )

    fun findAll(): List<Film> =
        jdbc.query(
            "SELECT * FROM films"
        ) { rs, _ ->
            Film(
                id = UUID.fromString(rs.getString("id")),
                title = rs.getString("title"),
                description = rs.getString("description"),
            )
        }

    fun findTopN(limit: Int, sortBy: String = "title"): List<Film> =
        jdbc.query(
            "SELECT * FROM films ORDER BY $sortBy LIMIT ?",
            { rs, _ ->
                Film(
                    id = UUID.fromString(rs.getString("id")),
                    title = rs.getString("title"),
                    description = rs.getString("description"),
                )
            },
            limit
        )

    fun save(film: Film) {
        jdbc.update(
            """
            INSERT INTO films (id, title, description)
            VALUES (?, ?, ?)
            ON CONFLICT (id) DO UPDATE
            SET title = ?, description = ?
            """,
            film.id.toString(), film.title, film.description,
            film.title, film.description
        )
    }

    fun deleteById(id: UUID) {
        jdbc.update(
            "DELETE FROM films WHERE id = ?",
            id.toString()
        )
    }
}
