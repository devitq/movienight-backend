package com.project.movienight.adapters.web.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

data class RateFilmRequest(
    @field:Min(1)
    @field:Max(10)
    val score: Int,
    @field:Size(max = 2048)
    val note: String? = null,
)
