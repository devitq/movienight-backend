package com.project.movienight.adapters.web.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import java.time.OffsetDateTime

data class JellyfinSyncRequest(
    @field:Valid
    val users: List<JellyfinSyncUserRequest> = emptyList(),
    @field:Valid
    val items: List<JellyfinSyncItemRequest> = emptyList(),
)

data class JellyfinSyncUserRequest(
    @field:NotBlank
    val jellyfinUserId: String,
    val name: String? = null,
)

data class JellyfinSyncItemRequest(
    @field:NotBlank
    val jellyfinItemId: String,
    @field:NotBlank
    val title: String,
    val originalTitle: String? = null,
    val description: String? = null,
    val year: Int? = null,
    val genres: List<String> = emptyList(),
    val imdbId: String? = null,
    val tmdbId: String? = null,
    val jellyfinLibraryId: String? = null,
    @field:Valid
    val userStates: List<JellyfinSyncUserStateRequest> = emptyList(),
)

data class JellyfinSyncUserStateRequest(
    @field:NotBlank
    val jellyfinUserId: String,
    val isViewed: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: OffsetDateTime? = null,
    val userRating: Double? = null,
)
