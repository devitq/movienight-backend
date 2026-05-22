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

    fun syncFromPlugin(command: JellyfinPluginSyncCommand): JellyfinSyncSummary

    fun getSyncStates(): List<JellyfinSyncState>
}

data class JellyfinPluginSyncCommand(
    val items: List<JellyfinPluginSyncItem>,
)

data class JellyfinPluginSyncItem(
    val jellyfinItemId: String,
    val title: String,
    val description: String?,
    val year: Int?,
    val genres: List<String>,
    val imdbId: String?,
    val userStates: List<JellyfinPluginUserState>,
)

data class JellyfinPluginUserState(
    val jellyfinUserId: String,
    val isViewed: Boolean,
    val lastPlayedAt: OffsetDateTime?,
)
