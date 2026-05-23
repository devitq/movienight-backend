package com.project.movienight.adapters.persistence.jdbc

import com.project.movienight.application.ports.output.FilmLibraryEntryRepositoryPort
import com.project.movienight.domain.model.FilmLibraryEntry
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

@Repository
class FilmLibraryEntryRepository(
    private val jdbc: JdbcTemplate,
) : FilmLibraryEntryRepositoryPort {
    private val rowMapper = { rs: ResultSet, _: Int ->
        FilmLibraryEntry(
            id = UUID.fromString(rs.getString("id")),
            userId = UUID.fromString(rs.getString("user_id")),
            filmId = UUID.fromString(rs.getString("film_id")),
            comment = rs.getString("comment"),
            isViewed = rs.getBoolean("is_viewed"),
            watchedAt = rs.getTimestamp("watched_at")?.toLocalDateTime(),
        )
    }

    override fun save(entry: FilmLibraryEntry): FilmLibraryEntry {
        val updatedRows =
            jdbc.update(
                """
                UPDATE favorites
                SET user_id = ?, film_id = ?, comment = ?, is_viewed = ?, watched_at = ?
                WHERE id = ?
                """.trimIndent(),
                entry.userId,
                entry.filmId,
                entry.comment,
                entry.isViewed,
                entry.watchedAt,
                entry.id,
            )
        if (updatedRows == 0) {
            jdbc.update(
                """
                INSERT INTO favorites (id, user_id, film_id, comment, is_viewed, watched_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                entry.id,
                entry.userId,
                entry.filmId,
                entry.comment,
                entry.isViewed,
                entry.watchedAt,
            )
        }
        return entry
    }

    override fun findById(id: UUID): FilmLibraryEntry? =
        jdbc
            .query(
                "SELECT id, user_id, film_id, comment, is_viewed, watched_at FROM favorites WHERE id = ?",
                rowMapper,
                id,
            ).firstOrNull()

    override fun findByUserId(userId: UUID): List<FilmLibraryEntry> =
        jdbc.query(
            "SELECT id, user_id, film_id, comment, is_viewed, watched_at FROM favorites WHERE user_id = ?",
            rowMapper,
            userId,
        )

    override fun findByUserIdAndFilmId(
        userId: UUID,
        filmId: UUID,
    ): FilmLibraryEntry? =
        jdbc
            .query(
                """
                SELECT id, user_id, film_id, comment, is_viewed, watched_at
                FROM favorites WHERE user_id = ? AND film_id = ?
                """.trimIndent(),
                rowMapper,
                userId,
                filmId,
            ).firstOrNull()

    override fun findAll(): List<FilmLibraryEntry> =
        jdbc.query(
            "SELECT id, user_id, film_id, comment, is_viewed, watched_at FROM favorites",
            rowMapper,
        )

    override fun deleteById(id: UUID) {
        jdbc.update("DELETE FROM favorites WHERE id = ?", id)
    }
}
