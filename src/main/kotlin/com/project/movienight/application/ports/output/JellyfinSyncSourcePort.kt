package com.project.movienight.application.ports.output

import com.project.movienight.domain.model.ContentType

data class JellyfinRemoteUser(
    val id: String,
    val name: String,
)

data class JellyfinLibraryItemSnapshot(
    val jellyfinItemId: String,
    val title: String,
    val description: String,
    val contentType: ContentType,
    val releaseYear: Int?,
    val genres: List<String>,
    val cast: List<String>,
    val directors: List<String>,
    val platformRating: Double?,
    val imdbRating: Double?,
    val externalUrl: String?,
    val jellyfinLibraryId: String?,
    val isPlayed: Boolean,
)

interface JellyfinSyncSourcePort {
    fun fetchUsers(): List<JellyfinRemoteUser>

    fun fetchLibraryItems(userId: String): List<JellyfinLibraryItemSnapshot>
}