package com.project.movienight.domain.model

data class RecommendationResult(
    val film: Film,
    val score: Double,
    val reasons: List<String>,
)