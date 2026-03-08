package com.project.movienight.domain.model

data class FilmLibrary(
    val id: Int,
    val userId: Int,
    val filmId: Int,
    val comment: String?,
    val isViewed: Boolean,
)
