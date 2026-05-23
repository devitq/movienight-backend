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

    fun ingest(command: IngestJellyfinSyncCommand): JellyfinSyncSummary

    fun getSyncStates(): List<JellyfinSyncState>
}

data class IngestJellyfinSyncCommand(
    val users: List<JellyfinSyncUserCommand> = emptyList(),
    val items: List<JellyfinSyncItemCommand> = emptyList(),
)

data class JellyfinSyncUserCommand(
    val jellyfinUserId: String,
    val name: String?,
)

data class JellyfinSyncItemCommand(
    val jellyfinItemId: String,
    val title: String,
    val originalTitle: String?,
    val description: String?,
    val year: Int?,
    val genres: List<String>,
    val imdbId: String?,
    val tmdbId: String?,
    val jellyfinLibraryId: String?,
    val userStates: List<JellyfinSyncUserStateCommand>,
)

data class JellyfinSyncUserStateCommand(
    val jellyfinUserId: String,
    val isViewed: Boolean,
    val playCount: Int,
    val lastPlayedAt: OffsetDateTime?,
    val userRating: Double?,
)
