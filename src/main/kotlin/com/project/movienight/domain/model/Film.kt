package com.project.movienight.domain.model

import java.time.LocalDate

data class Film(
    val id: Int,
    val title: String,
    val genreId: Int,
    val issueDate: LocalDate?,
)
