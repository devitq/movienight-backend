package com.project.movienight.application.ports.input

import com.project.movienight.domain.model.ContentType
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
    fun sync(command: PushJellyfinCatalogCommand): JellyfinSyncSummary

    fun getSyncStates(): List<JellyfinSyncState>
}

data class PushJellyfinCatalogCommand(
    val items: List<PushedJellyfinItem>,
)

data class PushedJellyfinItem(
    val jellyfinItemId: String,
    val jellyfinLibraryId: String?,
    val title: String,
    val description: String?,
    val contentType: ContentType,
    val releaseYear: Int?,
    val genres: List<String>,
    val cast: List<String>,
    val directors: List<String>,
    val platformRating: Double?,
    val imdbRating: Double?,
    val externalUrl: String?,
    val imdbId: String?,
    val userStates: List<PushedJellyfinUserState>,
)

data class PushedJellyfinUserState(
    val jellyfinUserId: String,
    val isViewed: Boolean,
    val lastPlayedAt: OffsetDateTime?,
)
