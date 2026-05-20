package com.project.movienight.adapters.persistence.jdbc

import com.project.movienight.adapters.persistence.entity.UserPreferencesEntity
import com.project.movienight.adapters.persistence.entity.toDomain
import com.project.movienight.adapters.persistence.entity.toEntity
import com.project.movienight.application.ports.output.UserPreferencesRepositoryPort
import com.project.movienight.domain.model.UserPreferences
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

@Repository
class UserPreferencesRepository(
    private val jdbc: JdbcTemplate,
) : UserPreferencesRepositoryPort {
    private val rowMapper = { rs: ResultSet, _: Int ->
        UserPreferencesEntity(
            userId = UUID.fromString(rs.getString("user_id")),
            weightedGenres = rs.getString("weighted_genres"),
            plotTypes = rs.getString("plot_types"),
            eras = rs.getString("eras"),
            castAndDirectors = rs.getString("cast_and_directors"),
            moods = rs.getString("moods"),
            contentTypes = rs.getString("content_types"),
        )
    }

    override fun save(preferences: UserPreferences): UserPreferences {
        val entity = preferences.toEntity()
        val updatedRows =
            jdbc.update(
                """
                UPDATE user_preferences
                SET weighted_genres = ?,
                    plot_types = ?,
                    eras = ?,
                    cast_and_directors = ?,
                    moods = ?,
                    content_types = ?
                WHERE user_id = ?
                """.trimIndent(),
                entity.weightedGenres,
                entity.plotTypes,
                entity.eras,
                entity.castAndDirectors,
                entity.moods,
                entity.contentTypes,
                entity.userId,
            )

        if (updatedRows == 0) {
            jdbc.update(
                """
                INSERT INTO user_preferences (
                    user_id,
                    weighted_genres,
                    plot_types,
                    eras,
                    cast_and_directors,
                    moods,
                    content_types
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                entity.userId,
                entity.weightedGenres,
                entity.plotTypes,
                entity.eras,
                entity.castAndDirectors,
                entity.moods,
                entity.contentTypes,
            )
        }

        return preferences
    }

    override fun findByUserId(userId: UUID): UserPreferences? =
        jdbc
            .query(
                """
                SELECT user_id,
                       weighted_genres,
                       plot_types,
                       eras,
                       cast_and_directors,
                       moods,
                       content_types
                FROM user_preferences
                WHERE user_id = ?
                """.trimIndent(),
                rowMapper,
                userId,
            ).firstOrNull()
            ?.toDomain()
}
