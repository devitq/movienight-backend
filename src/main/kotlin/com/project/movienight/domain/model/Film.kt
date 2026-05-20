package com.project.movienight.domain.model

import java.util.UUID

data class Film(
    val id: UUID,
    val title: String,
    val description: String,
    val contentType: ContentType = ContentType.FILM,
    val releaseYear: Int? = null,
    val genres: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val directors: List<String> = emptyList(),
    val imdbRating: Double? = null,
    val platformRating: Double? = null,
    val externalUrl: String? = null,
    val jellyfinItemId: String? = null,
    val jellyfinLibraryId: String? = null,
)

enum class ContentType {
    FILM,
    SERIES,
    EPISODE,
    OTHER,
}
