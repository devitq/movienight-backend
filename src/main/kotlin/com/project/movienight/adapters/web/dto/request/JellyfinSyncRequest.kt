package com.project.movienight.adapters.web.dto.request

import com.project.movienight.application.ports.input.JellyfinPluginSyncCommand
import com.project.movienight.application.ports.input.JellyfinPluginSyncItem
import com.project.movienight.application.ports.input.JellyfinPluginUserState
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import java.time.OffsetDateTime

data class JellyfinSyncRequest(
    @field:Valid
    val items: List<JellyfinSyncItemRequest> = emptyList(),
) {
    fun toCommand(): JellyfinPluginSyncCommand =
        JellyfinPluginSyncCommand(
            items = items.map { it.toCommand() },
        )
}

data class JellyfinSyncItemRequest(
    @field:NotBlank
    val jellyfinItemId: String,
    val title: String?,
    val description: String?,
    val year: Int?,
    val genres: List<String> = emptyList(),
    val imdbId: String?,
    @field:Valid
    val userStates: List<JellyfinUserStateRequest> = emptyList(),
) {
    fun toCommand(): JellyfinPluginSyncItem =
        JellyfinPluginSyncItem(
            jellyfinItemId = jellyfinItemId,
            title = title?.takeIf { it.isNotBlank() } ?: jellyfinItemId,
            description = description,
            year = year,
            genres = genres,
            imdbId = imdbId,
            userStates = userStates.map { it.toCommand() },
        )
}

data class JellyfinUserStateRequest(
    @field:NotBlank
    val jellyfinUserId: String,
    val isViewed: Boolean = false,
    val lastPlayedAt: OffsetDateTime? = null,
) {
    fun toCommand(): JellyfinPluginUserState =
        JellyfinPluginUserState(
            jellyfinUserId = jellyfinUserId,
            isViewed = isViewed,
            lastPlayedAt = lastPlayedAt,
        )
}
