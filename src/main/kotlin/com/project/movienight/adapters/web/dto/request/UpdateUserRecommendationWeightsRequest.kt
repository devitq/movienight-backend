package com.project.movienight.adapters.web.dto.request

data class UpdateUserRecommendationWeightsRequest(
    val relevanceWeight: Double,
    val qualityWeight: Double,
    val contextWeight: Double,
    val noveltyWeight: Double,
    val diversityWeight: Double,
    val genreVectorWeight: Double,
    val plotVectorWeight: Double,
    val moodVectorWeight: Double,
    val eraVectorWeight: Double,
    val peopleVectorWeight: Double,
    val contentTypeVectorWeight: Double,
)
