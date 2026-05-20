package com.project.movienight.application.services

import com.project.movienight.application.ports.input.RecommendationQuery
import com.project.movienight.application.ports.output.BusinessMetricsPort
import com.project.movienight.application.ports.output.FilmLibraryRepositoryPort
import com.project.movienight.application.ports.output.FilmRatingRepositoryPort
import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.application.ports.output.UserPreferencesRepositoryPort
import com.project.movienight.domain.model.Film
import com.project.movienight.domain.model.FilmRating
import com.project.movienight.domain.model.UserPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class RecommendationServiceTest {
    private val filmRepository: FilmRepositoryPort = mockk()
    private val filmLibraryRepository: FilmLibraryRepositoryPort = mockk()
    private val filmRatingRepository: FilmRatingRepositoryPort = mockk()
    private val userPreferencesRepository: UserPreferencesRepositoryPort = mockk()
    private val businessMetricsPort: BusinessMetricsPort = mockk(relaxed = true)

    private val recommendationService =
        RecommendationService(
            filmRepository = filmRepository,
            filmLibraryRepository = filmLibraryRepository,
            filmRatingRepository = filmRatingRepository,
            userPreferencesRepository = userPreferencesRepository,
            businessMetricsService = businessMetricsPort,
        )

    @Test
    fun `should rank highly rated films above poorly rated films`() {
        val userId = UUID.randomUUID()
        val lowRatedFilmId = UUID.randomUUID()
        val highRatedFilmId = UUID.randomUUID()

        val lowRatedFilm = Film(id = lowRatedFilmId, title = "Low", description = "Low")
        val highRatedFilm = Film(id = highRatedFilmId, title = "High", description = "High")

        every { userPreferencesRepository.findByUserId(userId) } returns UserPreferences(userId = userId)
        every { filmRatingRepository.findByUserId(userId) } returns
            listOf(
                FilmRating(id = UUID.randomUUID(), userId = userId, filmId = lowRatedFilmId, score = 1),
                FilmRating(id = UUID.randomUUID(), userId = userId, filmId = highRatedFilmId, score = 10),
            )
        every { filmLibraryRepository.findAll() } returns emptyList()
        every { filmRepository.findAll() } returns listOf(lowRatedFilm, highRatedFilm)

        val result = recommendationService.recommend(RecommendationQuery(userId = userId))

        assertEquals(highRatedFilmId, result.first().film.id)
    }
}