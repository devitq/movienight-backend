package com.project.movienight.application.services

import com.project.movienight.application.ports.input.FilmRatingUseCase
import com.project.movienight.application.ports.input.RateFilmCommand
import com.project.movienight.application.ports.output.BusinessMetricsPort
import com.project.movienight.application.ports.output.FilmRatingRepositoryPort
import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.domain.exception.DomainException
import com.project.movienight.domain.exception.EntityNotFoundException
import com.project.movienight.domain.model.FilmRating
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class FilmRatingService(
    private val filmRepository: FilmRepositoryPort,
    private val filmRatingRepository: FilmRatingRepositoryPort,
    private val idGenerator: IdGenerator,
    private val businessMetricsService: BusinessMetricsPort,
) : FilmRatingUseCase {
    override fun rate(command: RateFilmCommand): FilmRating {
        if (command.score !in 1..10) {
            throw DomainException("Film rating score must be between 1 and 10")
        }

        filmRepository.findById(command.filmId)
            ?: throw EntityNotFoundException(entity = "Film", id = command.filmId.toString())

        val existingRating = filmRatingRepository.findByUserIdAndFilmId(command.userId, command.filmId)
        val now = LocalDateTime.now()

        val rating =
            if (existingRating == null) {
                FilmRating(
                    id = idGenerator.generateId(),
                    userId = command.userId,
                    filmId = command.filmId,
                    score = command.score,
                    note = command.note,
                    createdAt = now,
                    updatedAt = now,
                )
            } else {
                existingRating.copy(score = command.score, note = command.note, updatedAt = now)
            }

        val savedRating = filmRatingRepository.save(rating)
        businessMetricsService.recordRatingSubmitted()
        return savedRating
    }

    override fun getRatings(userId: UUID): List<FilmRating> = filmRatingRepository.findByUserId(userId)
}
