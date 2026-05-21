package com.project.movienight.adapters.persistence.jdbc

import com.project.movienight.application.ports.output.RecommendationEventRepositoryPort
import com.project.movienight.domain.model.RecommendationEvent
import com.project.movienight.domain.model.RecommendationEventType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

@Repository
class RecommendationEventRepository(
    private val jdbc: JdbcTemplate,
) : RecommendationEventRepositoryPort {
    private val rowMapper = { rs: ResultSet, _: Int ->
        RecommendationEvent(
            id = UUID.fromString(rs.getString("id")),
            userId = UUID.fromString(rs.getString("user_id")),
            filmId = UUID.fromString(rs.getString("film_id")),
            eventType = RecommendationEventType.valueOf(rs.getString("event_type")),
            score = rs.getObject("score")?.let { (it as Number).toDouble() },
            relevanceScore = rs.getObject("relevance_score")?.let { (it as Number).toDouble() },
            qualityScore = rs.getObject("quality_score")?.let { (it as Number).toDouble() },
            contextScore = rs.getObject("context_score")?.let { (it as Number).toDouble() },
            noveltyScore = rs.getObject("novelty_score")?.let { (it as Number).toDouble() },
            diversityScore = rs.getObject("diversity_score")?.let { (it as Number).toDouble() },
            createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
        )
    }

    override fun save(event: RecommendationEvent): RecommendationEvent {
        jdbc.update(
            """
            INSERT INTO recommendation_events (
                id,
                user_id,
                film_id,
                event_type,
                score,
                relevance_score,
                quality_score,
                context_score,
                novelty_score,
                diversity_score,
                created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            event.id,
            event.userId,
            event.filmId,
            event.eventType.name,
            event.score,
            event.relevanceScore,
            event.qualityScore,
            event.contextScore,
            event.noveltyScore,
            event.diversityScore,
            event.createdAt,
        )
        return event
    }

    override fun findByUserId(userId: UUID): List<RecommendationEvent> =
        jdbc.query(
            """
            SELECT id,
                   user_id,
                   film_id,
                   event_type,
                   score,
                   relevance_score,
                   quality_score,
                   context_score,
                   novelty_score,
                   diversity_score,
                   created_at
            FROM recommendation_events
            WHERE user_id = ?
            ORDER BY created_at DESC
            """.trimIndent(),
            rowMapper,
            userId,
        )

    override fun findLatestRecommended(
        userId: UUID,
        filmId: UUID,
    ): RecommendationEvent? =
        jdbc
            .query(
                """
                SELECT id,
                       user_id,
                       film_id,
                       event_type,
                       score,
                       relevance_score,
                       quality_score,
                       context_score,
                       novelty_score,
                       diversity_score,
                       created_at
                FROM recommendation_events
                WHERE user_id = ?
                  AND film_id = ?
                  AND event_type = ?
                ORDER BY created_at DESC
                LIMIT 1
                """.trimIndent(),
                rowMapper,
                userId,
                filmId,
                RecommendationEventType.RECOMMENDED.name,
            ).firstOrNull()
}
