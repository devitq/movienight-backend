package com.project.movienight.adapters.web

import com.project.movienight.application.ports.input.GetRecommendationsUseCase
import com.project.movienight.application.ports.input.RecommendationQuery
import com.project.movienight.domain.model.ContentType
import com.project.movienight.domain.model.RecommendationResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/users/{userId}/recommendations")
class RecommendationController(
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
) {
    @GetMapping
    fun recommend(
        @PathVariable userId: UUID,
        @RequestParam(required = false) contentType: String?,
        @RequestParam(required = false) mood: String?,
        @RequestParam(required = false, defaultValue = "10") limit: Int,
    ): List<RecommendationResult> =
        getRecommendationsUseCase.recommend(
            RecommendationQuery(
                userId = userId,
                contentType = contentType?.let { runCatching { ContentType.valueOf(it) }.getOrNull() },
                mood = mood,
                limit = limit,
            ),
        )
}
