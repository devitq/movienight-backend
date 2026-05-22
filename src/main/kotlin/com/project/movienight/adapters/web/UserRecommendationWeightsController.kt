package com.project.movienight.adapters.web

import com.project.movienight.adapters.web.dto.request.UpdateUserRecommendationWeightsRequest
import com.project.movienight.adapters.web.dto.response.UserRecommendationWeightsResponse
import com.project.movienight.application.ports.input.GetUserRecommendationWeightsUseCase
import com.project.movienight.application.ports.input.UpdateUserRecommendationWeightsCommand
import com.project.movienight.application.ports.input.UpdateUserRecommendationWeightsUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/users/{userId}/recommendation-weights")
class UserRecommendationWeightsController(
    private val getUserRecommendationWeightsUseCase: GetUserRecommendationWeightsUseCase,
    private val updateUserRecommendationWeightsUseCase: UpdateUserRecommendationWeightsUseCase,
) {
    @GetMapping
    fun get(
        @PathVariable userId: UUID,
    ): UserRecommendationWeightsResponse =
        UserRecommendationWeightsResponse.fromDomain(
            getUserRecommendationWeightsUseCase.get(userId),
        )

    @PutMapping
    fun update(
        @PathVariable userId: UUID,
        @RequestBody request: UpdateUserRecommendationWeightsRequest,
    ): UserRecommendationWeightsResponse =
        UserRecommendationWeightsResponse.fromDomain(
            updateUserRecommendationWeightsUseCase.update(
                UpdateUserRecommendationWeightsCommand(
                    userId = userId,
                    relevanceWeight = request.relevanceWeight,
                    qualityWeight = request.qualityWeight,
                    contextWeight = request.contextWeight,
                    noveltyWeight = request.noveltyWeight,
                    diversityWeight = request.diversityWeight,
                    genreVectorWeight = request.genreVectorWeight,
                    plotVectorWeight = request.plotVectorWeight,
                    moodVectorWeight = request.moodVectorWeight,
                    eraVectorWeight = request.eraVectorWeight,
                    peopleVectorWeight = request.peopleVectorWeight,
                    contentTypeVectorWeight = request.contentTypeVectorWeight,
                ),
            ),
        )
}
