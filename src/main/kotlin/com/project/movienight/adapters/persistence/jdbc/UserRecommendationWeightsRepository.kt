package com.project.movienight.adapters.persistence.jdbc

import com.project.movienight.application.ports.output.UserRecommendationWeightsRepositoryPort
import com.project.movienight.domain.model.UserRecommendationWeights
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.UUID

@Repository
class UserRecommendationWeightsRepository(
    private val jdbc: JdbcTemplate,
) : UserRecommendationWeightsRepositoryPort {
    private val rowMapper = { rs: ResultSet, _: Int ->
        UserRecommendationWeights(
            userId = UUID.fromString(rs.getString("user_id")),
            relevanceWeight = rs.getDouble("relevance_weight"),
            qualityWeight = rs.getDouble("quality_weight"),
            contextWeight = rs.getDouble("context_weight"),
            noveltyWeight = rs.getDouble("novelty_weight"),
            diversityWeight = rs.getDouble("diversity_weight"),
            genreVectorWeight = rs.getDouble("genre_vector_weight"),
            plotVectorWeight = rs.getDouble("plot_vector_weight"),
            moodVectorWeight = rs.getDouble("mood_vector_weight"),
            eraVectorWeight = rs.getDouble("era_vector_weight"),
            peopleVectorWeight = rs.getDouble("people_vector_weight"),
            contentTypeVectorWeight = rs.getDouble("content_type_vector_weight"),
            updatedAt = rs.getTimestamp("updated_at").toLocalDateTime(),
        )
    }

    override fun findByUserId(userId: UUID): UserRecommendationWeights? =
        jdbc
            .query(
                """
                SELECT user_id,
                       relevance_weight,
                       quality_weight,
                       context_weight,
                       novelty_weight,
                       diversity_weight,
                       genre_vector_weight,
                       plot_vector_weight,
                       mood_vector_weight,
                       era_vector_weight,
                       people_vector_weight,
                       content_type_vector_weight,
                       updated_at
                FROM user_recommendation_weights
                WHERE user_id = ?
                """.trimIndent(),
                rowMapper,
                userId,
            ).firstOrNull()

    override fun save(weights: UserRecommendationWeights): UserRecommendationWeights {
        val normalized = weights.normalized(updatedAt = LocalDateTime.now())
        val updatedRows =
            jdbc.update(
                """
                UPDATE user_recommendation_weights
                SET relevance_weight = ?,
                    quality_weight = ?,
                    context_weight = ?,
                    novelty_weight = ?,
                    diversity_weight = ?,
                    genre_vector_weight = ?,
                    plot_vector_weight = ?,
                    mood_vector_weight = ?,
                    era_vector_weight = ?,
                    people_vector_weight = ?,
                    content_type_vector_weight = ?,
                    updated_at = ?
                WHERE user_id = ?
                """.trimIndent(),
                normalized.relevanceWeight,
                normalized.qualityWeight,
                normalized.contextWeight,
                normalized.noveltyWeight,
                normalized.diversityWeight,
                normalized.genreVectorWeight,
                normalized.plotVectorWeight,
                normalized.moodVectorWeight,
                normalized.eraVectorWeight,
                normalized.peopleVectorWeight,
                normalized.contentTypeVectorWeight,
                normalized.updatedAt,
                normalized.userId,
            )

        if (updatedRows == 0) {
            jdbc.update(
                """
                INSERT INTO user_recommendation_weights (
                    user_id,
                    relevance_weight,
                    quality_weight,
                    context_weight,
                    novelty_weight,
                    diversity_weight,
                    genre_vector_weight,
                    plot_vector_weight,
                    mood_vector_weight,
                    era_vector_weight,
                    people_vector_weight,
                    content_type_vector_weight,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                normalized.userId,
                normalized.relevanceWeight,
                normalized.qualityWeight,
                normalized.contextWeight,
                normalized.noveltyWeight,
                normalized.diversityWeight,
                normalized.genreVectorWeight,
                normalized.plotVectorWeight,
                normalized.moodVectorWeight,
                normalized.eraVectorWeight,
                normalized.peopleVectorWeight,
                normalized.contentTypeVectorWeight,
                normalized.updatedAt,
            )
        }

        return normalized
    }
}
