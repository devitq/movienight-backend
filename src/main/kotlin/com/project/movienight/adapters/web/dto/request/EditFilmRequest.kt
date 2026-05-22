package com.project.movienight.adapters.web.dto.request

data class EditFilmRequest(
    val title: String,
    val description: String,
    val contentType: String = "FILM",
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
