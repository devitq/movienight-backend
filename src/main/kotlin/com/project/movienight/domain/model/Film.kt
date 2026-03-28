package com.project.movienight.domain.model

import java.time.LocalDate
import java.util.UUID

data class Film(
    val id: UUID,
    val title: String,
    val description: String,
)
