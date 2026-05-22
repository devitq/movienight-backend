package com.project.movienight.adapters.web.dto.request

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.project.movienight.application.ports.input.PushJellyfinCatalogCommand
import com.project.movienight.application.ports.input.PushedJellyfinItem
import com.project.movienight.application.ports.input.PushedJellyfinUserState
import com.project.movienight.domain.exception.DomainException
import com.project.movienight.domain.model.ContentType
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import java.time.OffsetDateTime

data class JellyfinSyncRequest(
    @field:Valid
    val items: List<JellyfinSyncItemRequest> = emptyList(),
) {
    fun toCommand(): PushJellyfinCatalogCommand =
        PushJellyfinCatalogCommand(
            items = items.map { it.toCommand() },
        )
}

data class JellyfinSyncItemRequest(
    @JsonProperty("jellyfin_item_id")
    @JsonAlias("jellyfinItemId", "itemId", "Id")
    @field:NotBlank
    val jellyfinItemId: String,
    @JsonProperty("jellyfin_library_id")
    @JsonAlias("jellyfinLibraryId", "libraryId", "ParentId")
    val jellyfinLibraryId: String? = null,
    @JsonAlias("Name")
    val title: String? = null,
    @JsonAlias("Overview")
    val description: String? = null,
    @JsonProperty("content_type")
    @JsonAlias("contentType", "type", "Type")
    val contentType: String? = null,
    @JsonProperty("release_year")
    @JsonAlias("releaseYear", "ProductionYear")
    val releaseYear: Int? = null,
    val year: Int? = null,
    @JsonAlias("Genres")
    val genres: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val directors: List<String> = emptyList(),
    @JsonProperty("platform_rating")
    @JsonAlias("platformRating", "communityRating", "CommunityRating")
    val platformRating: Double? = null,
    @JsonProperty("imdb_rating")
    @JsonAlias("imdbRating")
    val imdbRating: Double? = null,
    @JsonProperty("external_url")
    @JsonAlias("externalUrl")
    val externalUrl: String? = null,
    @JsonProperty("imdb_id")
    @JsonAlias("imdbId")
    val imdbId: String? = null,
    @JsonProperty("user_states")
    @JsonAlias("userStates")
    @field:Valid
    val userStates: List<JellyfinUserStateRequest> = emptyList(),
) {
    fun toCommand(): PushedJellyfinItem =
        PushedJellyfinItem(
            jellyfinItemId = jellyfinItemId,
            jellyfinLibraryId = jellyfinLibraryId,
            title = title?.takeIf { it.isNotBlank() } ?: jellyfinItemId,
            description = description,
            contentType = parseContentType(contentType),
            releaseYear = releaseYear ?: year,
            genres = genres,
            cast = cast,
            directors = directors,
            platformRating = platformRating,
            imdbRating = imdbRating,
            externalUrl = externalUrl,
            imdbId = imdbId,
            userStates = userStates.map { it.toCommand() },
        )
}

data class JellyfinUserStateRequest(
    @JsonProperty("jellyfin_user_id")
    @JsonAlias("jellyfinUserId", "userId", "UserId")
    @field:NotBlank
    val jellyfinUserId: String,
    @JsonProperty("is_viewed")
    @JsonAlias("isViewed", "played", "Played", "IsPlayed")
    val isViewed: Boolean = false,
    @JsonProperty("last_played_at")
    @JsonAlias("lastPlayedAt", "LastPlayedDate")
    val lastPlayedAt: OffsetDateTime? = null,
) {
    fun toCommand(): PushedJellyfinUserState =
        PushedJellyfinUserState(
            jellyfinUserId = jellyfinUserId,
            isViewed = isViewed,
            lastPlayedAt = lastPlayedAt,
        )
}

private fun parseContentType(value: String?): ContentType =
    when (value?.trim()?.lowercase()) {
        null, "", "movie", "film" -> ContentType.FILM
        "series" -> ContentType.SERIES
        "episode" -> ContentType.EPISODE
        "other" -> ContentType.OTHER
        else -> throw DomainException("Unsupported Jellyfin content type: $value")
    }
