package com.project.movienight.adapters.persistence.jdbc

import com.project.movienight.adapters.persistence.entity.FilmRatingEntity
import com.project.movienight.adapters.persistence.entity.toDomain
import com.project.movienight.adapters.persistence.entity.toEntity
import com.project.movienight.application.ports.output.FilmRatingRepositoryPort
import com.project.movienight.domain.model.FilmRating
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.UUID

@Repository
class FilmRatingRepository(
    private val jdbc: JdbcTemplate,
) : FilmRatingRepositoryPort {
    private val rowMapper = { rs: ResultSet, _: Int ->
        FilmRatingEntity(
            id = UUID.fromString(rs.getString("id")),
            userId = UUID.fromString(rs.getString("user_id")),
            filmId = UUID.fromString(rs.getString("film_id")),
            score = rs.getInt("score"),
            note = rs.getString("note"),
            createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
            updatedAt = rs.getTimestamp("updated_at").toLocalDateTime(),
        )
    }

    override fun save(rating: FilmRating): FilmRating {
        val entity = rating.toEntity()
        val updatedRows =
            jdbc.update(
                """
                UPDATE film_ratings
                SET score = ?,
                    note = ?,
                    updated_at = ?
                WHERE user_id = ?
                  AND film_id = ?
                """.trimIndent(),
                entity.score,
                entity.note,
                LocalDateTime.now(),
                entity.userId,
                entity.filmId,
            )

        if (updatedRows == 0) {
            jdbc.update(
                """
                INSERT INTO film_ratings (
                    id,
                    user_id,
                    film_id,
                    score,
                    note,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                entity.id,
                entity.userId,
                entity.filmId,
                entity.score,
                entity.note,
                entity.createdAt,
                entity.updatedAt,
            )
        }

        return rating
    }

    override fun findByUserId(userId: UUID): List<FilmRating> =
        jdbc
            .query(
                """
                SELECT id,
                       user_id,
                       film_id,
                       score,
                       note,
                       created_at,
                       updated_at
                FROM film_ratings
                WHERE user_id = ?
                """.trimIndent(),
                rowMapper,
                userId,
            ).map { it.toDomain() }

    override fun findByUserIdAndFilmId(
        userId: UUID,
        filmId: UUID,
    ): FilmRating? =
        jdbc
            .query(
                """
                SELECT id,
                       user_id,
                       film_id,
                       score,
                       note,
                       created_at,
                       updated_at
                FROM film_ratings
                WHERE user_id = ?
                  AND film_id = ?
                """.trimIndent(),
                rowMapper,
                userId,
                filmId,
            ).firstOrNull()
            ?.toDomain()
}
