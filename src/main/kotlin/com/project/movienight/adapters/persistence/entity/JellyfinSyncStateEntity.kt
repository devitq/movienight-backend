package com.project.movienight.adapters.persistence.entity

import com.project.movienight.domain.model.JellyfinSyncState
import java.time.LocalDateTime
import java.util.UUID

data class JellyfinSyncStateEntity(
    val userId: UUID,
    val lastSyncedAt: LocalDateTime?,
    val lastSuccessfulSyncAt: LocalDateTime?,
    val lastError: String?,
    val syncedItemCount: Int,
)

fun JellyfinSyncStateEntity.toDomain(): JellyfinSyncState =
    JellyfinSyncState(
        userId = userId,
        lastSyncedAt = lastSyncedAt,
        lastSuccessfulSyncAt = lastSuccessfulSyncAt,
        lastError = lastError,
        syncedItemCount = syncedItemCount,
    )

fun JellyfinSyncState.toEntity(): JellyfinSyncStateEntity =
    JellyfinSyncStateEntity(
        userId = userId,
        lastSyncedAt = lastSyncedAt,
        lastSuccessfulSyncAt = lastSuccessfulSyncAt,
        lastError = lastError,
        syncedItemCount = syncedItemCount,
    )