package com.project.movienight.adapters.persistence

import com.project.movienight.domain.model.Film
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class FilmRepository(private val jdbc: JdbcTemplate) {

    fun findById(id: Int): Film? =
        jdbc.queryForObject(
            "SELECT * FROM films WHERE id = ?",
            { rs, _ ->
                Film(
                    id = rs.getInt("id"),
                    title = rs.getString("title"),
                    genreId = rs.getInt("genre_id"),
                    issueDate = rs.getDate("issue_date")?.toLocalDate(),
                )
            },
            id
        )

    fun findAll(): List<Film> =
        jdbc.query(
            "SELECT * FROM films"
        ) { rs, _ ->
            Film(
                id = rs.getInt("id"),
                title = rs.getString("title"),
                genreId = rs.getInt("genre_id"),
                issueDate = rs.getDate("issue_date")?.toLocalDate(),
            )
        }

    fun findTopN(limit: Int, sortBy: String = "title"): List<Film> =
        jdbc.query(
            "SELECT * FROM films ORDER BY $sortBy LIMIT ?",
            { rs, _ ->
                Film(
                    id = rs.getInt("id"),
                    title = rs.getString("title"),
                    genreId = rs.getInt("genre_id"),
                    issueDate = rs.getDate("issue_date")?.toLocalDate(),
                )
            },
            limit
        )

    fun save(film: Film) {
        jdbc.update(
            """
            INSERT INTO films (title, genre_id, issue_date)
            VALUES (?, ?, ?)
            ON CONFLICT (id) DO UPDATE
            SET title = ?, genre_id = ?, issue_date = ?
            """,
            film.title, film.genreId, film.issueDate,
            film.title, film.genreId, film.issueDate
        )
    }

    fun deleteById(id: Int) {
        jdbc.update(
            "DELETE FROM films WHERE id = ?",
            id
        )
    }
}
