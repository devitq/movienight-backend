package com.project.movienight.adapters.persistence.jdbc

import com.project.movienight.application.ports.output.JellyfinEventRecord
import com.project.movienight.application.ports.output.JellyfinEventStorePort
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class JellyfinEventRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) : JellyfinEventStorePort {
    override fun save(event: JellyfinEventRecord): Boolean {
        val sql =
            """
            INSERT INTO jellyfin_events(event_id, server_id, event_type, occurred_at, jellyfin_user_id, jellyfin_item_id, payload)
            VALUES (:eventId, :serverId, :eventType, :occurredAt, :jellyfinUserId, :jellyfinItemId, cast(:payload as jsonb))
            ON CONFLICT (event_id) DO NOTHING
            """.trimIndent()

        val params =
            MapSqlParameterSource()
                .addValue("eventId", event.eventId)
                .addValue("serverId", event.serverId)
                .addValue("eventType", event.eventType)
                .addValue("occurredAt", event.occurredAt)
                .addValue("jellyfinUserId", event.jellyfinUserId)
                .addValue("jellyfinItemId", event.jellyfinItemId)
                .addValue("payload", event.payload)

        return jdbc.update(sql, params) == 1
    }
}
