package com.project.movienight.application.services

import com.project.movienight.application.ports.input.GetUserRecommendationWeightsUseCase
import com.project.movienight.application.ports.input.UpdateUserRecommendationWeightsCommand
import com.project.movienight.application.ports.input.UpdateUserRecommendationWeightsUseCase
import com.project.movienight.application.ports.output.UserRecommendationWeightsRepositoryPort
import com.project.movienight.application.ports.output.UserRepositoryPort
import com.project.movienight.domain.exception.EntityNotFoundException
import com.project.movienight.domain.model.UserRecommendationWeights
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserRecommendationWeightsService(
    private val userRecommendationWeightsRepository: UserRecommendationWeightsRepositoryPort,
    private val userRepository: UserRepositoryPort,
) : GetUserRecommendationWeightsUseCase,
    UpdateUserRecommendationWeightsUseCase {
    override fun get(userId: UUID): UserRecommendationWeights {
        ensureUserExists(userId)
        return (
            userRecommendationWeightsRepository.findByUserId(userId)
                ?: UserRecommendationWeights.defaultFor(userId)
        ).normalized()
    }

    override fun update(command: UpdateUserRecommendationWeightsCommand): UserRecommendationWeights {
        ensureUserExists(command.userId)
        return userRecommendationWeightsRepository.save(
            UserRecommendationWeights(
                userId = command.userId,
                relevanceWeight = command.relevanceWeight,
                qualityWeight = command.qualityWeight,
                contextWeight = command.contextWeight,
                noveltyWeight = command.noveltyWeight,
                diversityWeight = command.diversityWeight,
                genreVectorWeight = command.genreVectorWeight,
                plotVectorWeight = command.plotVectorWeight,
                moodVectorWeight = command.moodVectorWeight,
                eraVectorWeight = command.eraVectorWeight,
                peopleVectorWeight = command.peopleVectorWeight,
                contentTypeVectorWeight = command.contentTypeVectorWeight,
            ),
        )
    }

    private fun ensureUserExists(userId: UUID) {
        userRepository.findById(userId)
            ?: throw EntityNotFoundException(entity = "User", id = userId.toString())
    }
}
