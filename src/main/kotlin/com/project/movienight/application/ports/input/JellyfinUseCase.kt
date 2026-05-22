package com.project.movienight.application.ports.input

import com.project.movienight.domain.model.JellyfinSyncState
import com.project.movienight.domain.model.JellyfinSyncSummary
import java.time.OffsetDateTime

interface JellyfinEventUseCase {
    fun handle(command: HandleJellyfinEventCommand)
}

data class HandleJellyfinEventCommand(
    val eventId: String,
    val serverId: String?,
    val eventType: String,
    val occurredAt: OffsetDateTime,
    val jellyfinUserId: String,
    val itemId: String,
    val payload: Map<String, Any>?,
)

interface JellyfinSyncUseCase {
    fun syncNow(): JellyfinSyncSummary

    fun getSyncStates(): List<JellyfinSyncState>
}
