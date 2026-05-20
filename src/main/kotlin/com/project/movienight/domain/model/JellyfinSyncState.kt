package com.project.movienight.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class JellyfinSyncState(
    val userId: UUID,
    val lastSyncedAt: LocalDateTime? = null,
    val lastSuccessfulSyncAt: LocalDateTime? = null,
    val lastError: String? = null,
    val syncedItemCount: Int = 0,
)

data class JellyfinSyncSummary(
    val syncedUsers: Int,
    val skippedUsers: Int,
    val syncedItems: Int,
    val durationMs: Long,
)
