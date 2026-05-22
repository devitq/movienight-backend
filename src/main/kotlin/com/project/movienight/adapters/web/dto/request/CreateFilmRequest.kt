package com.project.movienight.adapters.web.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateFilmRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val title: String,
    @field:NotBlank
    val description: String,
    @field:NotBlank
    val contentType: String = "FILM",
    @field:Min(1888)
    @field:Max(3000)
    val releaseYear: Int? = null,
    val genres: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val directors: List<String> = emptyList(),
    @field:Min(0)
    @field:Max(10)
    val imdbRating: Double? = null,
    @field:Min(0)
    @field:Max(10)
    val platformRating: Double? = null,
    val externalUrl: String? = null,
    val jellyfinItemId: String? = null,
    val jellyfinLibraryId: String? = null,
)
