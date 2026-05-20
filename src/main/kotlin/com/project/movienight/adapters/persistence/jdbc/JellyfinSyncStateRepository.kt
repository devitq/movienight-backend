package com.project.movienight.adapters.persistence.jdbc

import com.project.movienight.adapters.persistence.entity.JellyfinSyncStateEntity
import com.project.movienight.adapters.persistence.entity.toDomain
import com.project.movienight.adapters.persistence.entity.toEntity
import com.project.movienight.application.ports.output.JellyfinSyncStateRepositoryPort
import com.project.movienight.domain.model.JellyfinSyncState
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

@Repository
class JellyfinSyncStateRepository(
    private val jdbc: JdbcTemplate,
) : JellyfinSyncStateRepositoryPort {
    private val rowMapper = { rs: ResultSet, _: Int ->
        JellyfinSyncStateEntity(
            userId = UUID.fromString(rs.getString("user_id")),
            lastSyncedAt = rs.getTimestamp("last_synced_at")?.toLocalDateTime(),
            lastSuccessfulSyncAt = rs.getTimestamp("last_successful_sync_at")?.toLocalDateTime(),
            lastError = rs.getString("last_error"),
            syncedItemCount = rs.getInt("synced_item_count"),
        )
    }

    override fun save(state: JellyfinSyncState): JellyfinSyncState {
        val entity = state.toEntity()
        val updatedRows =
            jdbc.update(
                """
                UPDATE jellyfin_sync_state
                SET last_synced_at = ?,
                    last_successful_sync_at = ?,
                    last_error = ?,
                    synced_item_count = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                """.trimIndent(),
                entity.lastSyncedAt,
                entity.lastSuccessfulSyncAt,
                entity.lastError,
                entity.syncedItemCount,
                entity.userId,
            )

        if (updatedRows == 0) {
            jdbc.update(
                """
                INSERT INTO jellyfin_sync_state (
                    user_id,
                    last_synced_at,
                    last_successful_sync_at,
                    last_error,
                    synced_item_count
                )
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent(),
                entity.userId,
                entity.lastSyncedAt,
                entity.lastSuccessfulSyncAt,
                entity.lastError,
                entity.syncedItemCount,
            )
        }

        return state
    }

    override fun findByUserId(userId: UUID): JellyfinSyncState? =
        jdbc
            .query(
                """
                SELECT user_id,
                       last_synced_at,
                       last_successful_sync_at,
                       last_error,
                       synced_item_count
                FROM jellyfin_sync_state
                WHERE user_id = ?
                """.trimIndent(),
                rowMapper,
                userId,
            ).firstOrNull()
            ?.toDomain()

    override fun findAll(): List<JellyfinSyncState> =
        jdbc
            .query(
                """
                SELECT user_id,
                       last_synced_at,
                       last_successful_sync_at,
                       last_error,
                       synced_item_count
                FROM jellyfin_sync_state
                """.trimIndent(),
                rowMapper,
            ).map { it.toDomain() }
}
