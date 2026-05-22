package com.project.movienight.adapters.web

import com.project.movienight.adapters.web.dto.request.RecommendationOnboardingRequest
import com.project.movienight.adapters.web.dto.response.RecommendationOnboardingResponse
import com.project.movienight.application.ports.input.CompleteRecommendationOnboardingCommand
import com.project.movienight.application.ports.input.CompleteRecommendationOnboardingUseCase
import com.project.movienight.domain.model.ContentType
import com.project.movienight.domain.model.RecommendationStyle
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Locale
import java.util.UUID

@RestController
@RequestMapping("/api/users/{userId}/recommendation-onboarding")
class RecommendationOnboardingController(
    private val completeRecommendationOnboardingUseCase: CompleteRecommendationOnboardingUseCase,
) {
    @PostMapping
    fun complete(
        @PathVariable userId: UUID,
        @RequestBody request: RecommendationOnboardingRequest,
    ): RecommendationOnboardingResponse =
        RecommendationOnboardingResponse.fromApplication(
            completeRecommendationOnboardingUseCase.complete(
                CompleteRecommendationOnboardingCommand(
                    userId = userId,
                    weightedGenres = request.weightedGenres,
                    plotTypes = request.plotTypes,
                    eras = request.eras,
                    castAndDirectors = request.castAndDirectors,
                    moods = request.moods,
                    contentTypes = request.contentTypes.mapNotNull(::parseContentType),
                    likedFilmIds = request.likedFilmIds,
                    dislikedFilmIds = request.dislikedFilmIds,
                    libraryFilmIds = request.libraryFilmIds,
                    watchedFilmIds = request.watchedFilmIds,
                    recommendationStyle = parseRecommendationStyle(request.recommendationStyle),
                ),
            ),
        )

    private fun parseContentType(value: String): ContentType? =
        runCatching { ContentType.valueOf(value.uppercase(Locale.getDefault())) }.getOrNull()

    private fun parseRecommendationStyle(value: String): RecommendationStyle =
        runCatching { RecommendationStyle.valueOf(value.uppercase(Locale.getDefault())) }
            .getOrDefault(RecommendationStyle.BALANCED)
}
