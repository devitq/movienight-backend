package com.project.movienight.domain.model

import java.util.UUID

data class User(
    val id: UUID,
    val name: String,
    val email: String,
    val library: FilmLibrary?,
)
