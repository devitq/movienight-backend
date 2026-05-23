package com.project.movienight.adapters.web

import com.project.movienight.adapters.web.dto.response.RecommendationEventResponse
import com.project.movienight.adapters.web.dto.response.RecommendationResponse
import com.project.movienight.application.ports.input.AcceptRecommendationCommand
import com.project.movienight.application.ports.input.AcceptRecommendationUseCase
import com.project.movienight.application.ports.input.GetRecommendationsUseCase
import com.project.movienight.application.ports.input.RecommendationQuery
import com.project.movienight.application.ports.input.RejectRecommendationCommand
import com.project.movienight.application.ports.input.RejectRecommendationUseCase
import com.project.movienight.config.JellyfinIntegrationProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

@RestController
@RequestMapping("/api/users/{userId}/recommendations")
class RecommendationController(
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val acceptRecommendationUseCase: AcceptRecommendationUseCase,
    private val rejectRecommendationUseCase: RejectRecommendationUseCase,
    private val jellyfinProperties: JellyfinIntegrationProperties,
) {
    @GetMapping
    fun recommend(
        @PathVariable userId: UUID,
        @RequestParam(required = false) contentType: String?,
        @RequestParam(required = false) mood: String?,
        @RequestParam(required = false, defaultValue = "false") libraryOnly: Boolean,
        @RequestParam(required = false, defaultValue = "10") limit: Int,
    ): List<RecommendationResponse> =
        getRecommendationsUseCase
            .recommend(
                RecommendationQuery(
                    userId = userId,
                    contentType = parseOptionalContentType(contentType),
                    mood = mood,
                    libraryOnly = libraryOnly,
                    limit = limit,
                ),
            ).map { recommendation ->
                RecommendationResponse.fromDomain(
                    recommendation = recommendation,
                    watchUrl = buildWatchUrl(recommendation.film.jellyfinItemId),
                )
            }

    @PostMapping("/{filmId}/accept")
    fun accept(
        @PathVariable userId: UUID,
        @PathVariable filmId: UUID,
    ): RecommendationEventResponse =
        RecommendationEventResponse.fromDomain(
            acceptRecommendationUseCase.accept(
                AcceptRecommendationCommand(
                    userId = userId,
                    filmId = filmId,
                ),
            ),
        )

    @PostMapping("/{filmId}/reject")
    fun reject(
        @PathVariable userId: UUID,
        @PathVariable filmId: UUID,
    ): RecommendationEventResponse =
        RecommendationEventResponse.fromDomain(
            rejectRecommendationUseCase.reject(
                RejectRecommendationCommand(
                    userId = userId,
                    filmId = filmId,
                ),
            ),
        )

    private fun buildWatchUrl(jellyfinItemId: String?): String? {
        if (jellyfinItemId.isNullOrBlank() || jellyfinProperties.webUrl.isBlank()) {
            return null
        }

        val baseUrl = jellyfinProperties.webUrl.trimEnd('/')
        val encodedItemId = URLEncoder.encode(jellyfinItemId, StandardCharsets.UTF_8)
        return "$baseUrl/web/#/details?id=$encodedItemId"
    }
}
