package com.project.movienight.application.services

import com.project.movienight.application.ports.input.CompleteRecommendationOnboardingCommand
import com.project.movienight.application.ports.input.CompleteRecommendationOnboardingUseCase
import com.project.movienight.application.ports.input.RecommendationOnboardingResult
import com.project.movienight.application.ports.output.FilmLibraryEntryRepositoryPort
import com.project.movienight.application.ports.output.FilmRatingRepositoryPort
import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.application.ports.output.UserPreferencesRepositoryPort
import com.project.movienight.application.ports.output.UserRecommendationWeightsRepositoryPort
import com.project.movienight.application.ports.output.UserRepositoryPort
import com.project.movienight.domain.exception.EntityNotFoundException
import com.project.movienight.domain.model.FilmLibraryEntry
import com.project.movienight.domain.model.FilmRating
import com.project.movienight.domain.model.UserPreferences
import com.project.movienight.domain.model.UserRecommendationWeights
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class RecommendationOnboardingService(
    private val userRepository: UserRepositoryPort,
    private val filmRepository: FilmRepositoryPort,
    private val userPreferencesRepository: UserPreferencesRepositoryPort,
    private val filmRatingRepository: FilmRatingRepositoryPort,
    private val filmLibraryEntryRepository: FilmLibraryEntryRepositoryPort,
    private val userRecommendationWeightsRepository: UserRecommendationWeightsRepositoryPort,
    private val idGenerator: IdGenerator,
) : CompleteRecommendationOnboardingUseCase {
    override fun complete(command: CompleteRecommendationOnboardingCommand): RecommendationOnboardingResult {
        userRepository.findById(command.userId)
            ?: throw EntityNotFoundException(entity = "User", id = command.userId.toString())

        val filmIds =
            (
                command.likedFilmIds +
                    command.dislikedFilmIds +
                    command.libraryFilmIds +
                    command.watchedFilmIds
            ).distinct()
        ensureFilmsExist(filmIds)

        val preferences =
            userPreferencesRepository.save(
                UserPreferences(
                    userId = command.userId,
                    weightedGenres = command.weightedGenres,
                    plotTypes = command.plotTypes,
                    eras = command.eras,
                    castAndDirectors = command.castAndDirectors,
                    moods = command.moods,
                    contentTypes = command.contentTypes,
                ),
            )

        command.likedFilmIds.distinct().forEach { filmId ->
            saveRating(userId = command.userId, filmId = filmId, score = LIKED_SCORE, note = ONBOARDING_LIKED_NOTE)
        }
        command.dislikedFilmIds.distinct().forEach { filmId ->
            saveRating(
                userId = command.userId,
                filmId = filmId,
                score = DISLIKED_SCORE,
                note = ONBOARDING_DISLIKED_NOTE,
            )
        }
        command.libraryFilmIds.distinct().forEach { filmId ->
            saveLibraryEntry(userId = command.userId, filmId = filmId, isViewed = false)
        }
        command.watchedFilmIds.distinct().forEach { filmId ->
            saveLibraryEntry(userId = command.userId, filmId = filmId, isViewed = true)
        }

        val weights =
            userRecommendationWeightsRepository.save(
                UserRecommendationWeights.forStyle(
                    userId = command.userId,
                    style = command.recommendationStyle,
                ),
            )

        return RecommendationOnboardingResult(
            userId = command.userId,
            preferences = preferences,
            weights = weights,
            likedFilmsCount = command.likedFilmIds.distinct().size,
            dislikedFilmsCount = command.dislikedFilmIds.distinct().size,
            libraryFilmsCount = command.libraryFilmIds.distinct().size,
            watchedFilmsCount = command.watchedFilmIds.distinct().size,
        )
    }

    private fun ensureFilmsExist(filmIds: List<UUID>) {
        filmIds.forEach { filmId ->
            filmRepository.findById(filmId)
                ?: throw EntityNotFoundException(entity = "Film", id = filmId.toString())
        }
    }

    private fun saveRating(
        userId: UUID,
        filmId: UUID,
        score: Int,
        note: String,
    ): FilmRating {
        val now = LocalDateTime.now()
        val existing = filmRatingRepository.findByUserIdAndFilmId(userId, filmId)
        return filmRatingRepository.save(
            existing?.copy(score = score, note = note, updatedAt = now)
                ?: FilmRating(
                    id = idGenerator.generateId(),
                    userId = userId,
                    filmId = filmId,
                    score = score,
                    note = note,
                    createdAt = now,
                    updatedAt = now,
                ),
        )
    }

    private fun saveLibraryEntry(
        userId: UUID,
        filmId: UUID,
        isViewed: Boolean,
    ): FilmLibraryEntry {
        val watchedAt = LocalDateTime.now().takeIf { isViewed }
        val existing = filmLibraryEntryRepository.findByUserIdAndFilmId(userId, filmId)
        return filmLibraryEntryRepository.save(
            existing?.copy(isViewed = isViewed, watchedAt = watchedAt)
                ?: FilmLibraryEntry(
                    id = idGenerator.generateId(),
                    userId = userId,
                    filmId = filmId,
                    comment = null,
                    isViewed = isViewed,
                    watchedAt = watchedAt,
                ),
        )
    }

    private companion object {
        private const val LIKED_SCORE = 10
        private const val DISLIKED_SCORE = 2
        private const val ONBOARDING_LIKED_NOTE = "Onboarding liked"
        private const val ONBOARDING_DISLIKED_NOTE = "Onboarding disliked"
    }
}
