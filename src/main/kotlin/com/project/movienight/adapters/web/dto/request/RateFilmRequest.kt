package com.project.movienight.adapters.web.dto.request

data class RateFilmRequest(
    val score: Int,
    val note: String? = null,
)
