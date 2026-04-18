package com.project.movienight.adapters.persistence.jdbc

import com.project.movienight.application.ports.output.FilmLibraryRepositoryPort
import com.project.movienight.domain.model.FilmLibrary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

@Repository
class FilmLibraryRepository(
    private val jdbc: JdbcTemplate,
) : FilmLibraryRepositoryPort {
    private val filmLibraryRowMapper = { rs: ResultSet, _: Int ->
        FilmLibrary(
            id = UUID.fromString(rs.getString("id")),
            userId = UUID.fromString(rs.getString("user_id")),
            filmId = UUID.fromString(rs.getString("film_id")),
            comment = rs.getString("comment"),
            isViewed = rs.getBoolean("is_viewed"),
        )
    }

    override fun save(filmLibrary: FilmLibrary): FilmLibrary {
        val updatedRows =
            jdbc.update(
                """
                UPDATE favorites
                SET user_id = ?, film_id = ?, comment = ?, is_viewed = ?
                WHERE id = ?
                """.trimIndent(),
                filmLibrary.userId,
                filmLibrary.filmId,
                filmLibrary.comment,
                filmLibrary.isViewed,
                filmLibrary.id,
            )
        if (updatedRows == 0) {
            jdbc.update(
                """
                INSERT INTO favorites (id, user_id, film_id, comment, is_viewed)
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent(),
                filmLibrary.id,
                filmLibrary.userId,
                filmLibrary.filmId,
                filmLibrary.comment,
                filmLibrary.isViewed,
            )
        }
        return filmLibrary
    }

    override fun findById(id: UUID): FilmLibrary? {
        val entries =
            jdbc.query(
                "SELECT id, user_id, film_id, comment, is_viewed FROM favorites WHERE id = ?",
                filmLibraryRowMapper,
                id,
            )
        return entries.firstOrNull()
    }

    override fun findAll(): List<FilmLibrary> =
        jdbc.query(
            "SELECT id, user_id, film_id, comment, is_viewed FROM favorites",
            filmLibraryRowMapper,
        )

    override fun deleteById(id: UUID) {
        jdbc.update("DELETE FROM favorites WHERE id = ?", id)
    }
}
