package com.project.movienight.adapters.persistence.jdbc

import com.project.movienight.application.ports.output.JellyfinEventRepositoryPort
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class JellyfinEventRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) : JellyfinEventRepositoryPort {
    override fun save(
        eventId: String,
        serverId: String?,
        eventType: String,
        occurredAt: java.time.OffsetDateTime?,
        jellyfinUserId: String?,
        jellyfinItemId: String?,
        payload: String?,
    ): Int {
        val sql =
            """
            INSERT INTO jellyfin_events(event_id, server_id, event_type, occurred_at, jellyfin_user_id, jellyfin_item_id, payload)
            VALUES (:eventId, :serverId, :eventType, :occurredAt, :jellyfinUserId, :jellyfinItemId, cast(:payload as jsonb))
            ON CONFLICT (event_id) DO NOTHING
            """.trimIndent()

        val params =
            MapSqlParameterSource()
                .addValue("eventId", eventId)
                .addValue("serverId", serverId)
                .addValue("eventType", eventType)
                .addValue("occurredAt", occurredAt)
                .addValue("jellyfinUserId", jellyfinUserId)
                .addValue("jellyfinItemId", jellyfinItemId)
                .addValue("payload", payload)

        return jdbc.update(sql, params)
    }

    override fun delete(eventId: String) {
        val sql = "DELETE FROM jellyfin_events WHERE event_id = :eventId"
        val params = MapSqlParameterSource().addValue("eventId", eventId)
        jdbc.update(sql, params)
    }
}
