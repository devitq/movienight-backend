package com.project.movienight.application.services

import com.project.movienight.adapters.metrics.BusinessMetricsService
import com.project.movienight.application.ports.input.GetRecommendationsUseCase
import com.project.movienight.application.ports.input.RecommendationQuery
import com.project.movienight.application.ports.output.FilmLibraryRepositoryPort
import com.project.movienight.application.ports.output.FilmRatingRepositoryPort
import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.application.ports.output.UserPreferencesRepositoryPort
import com.project.movienight.domain.model.ContentType
import com.project.movienight.domain.model.Film
import com.project.movienight.domain.model.RecommendationResult
import org.springframework.stereotype.Service

@Service
class RecommendationService(
    private val filmRepository: FilmRepositoryPort,
    private val filmLibraryRepository: FilmLibraryRepositoryPort,
    private val filmRatingRepository: FilmRatingRepositoryPort,
    private val userPreferencesRepository: UserPreferencesRepositoryPort,
    private val businessMetricsService: BusinessMetricsService,
) : GetRecommendationsUseCase {
    override fun recommend(query: RecommendationQuery): List<RecommendationResult> {
        businessMetricsService.recordRecommendationRequest()
        val preferences = userPreferencesRepository.findByUserId(query.userId)
        val ratings = filmRatingRepository.findByUserId(query.userId).associateBy { it.filmId }
        val watchedFilmIds =
            filmLibraryRepository
                .findAll()
                .filter {
                    it.userId == query.userId && it.isViewed
                }.map { it.filmId }
                .toSet()

        return filmRepository
            .findAll()
            .asSequence()
            .filter { film -> query.contentType == null || film.contentType == query.contentType }
            .map { film ->
                scoreFilm(film, query.mood, preferences, ratings[film.id] != null, watchedFilmIds.contains(film.id))
            }.sortedByDescending { it.score }
            .take(query.limit.coerceAtLeast(1))
            .toList()
    }

    private fun scoreFilm(
        film: Film,
        mood: String?,
        preferences: com.project.movienight.domain.model.UserPreferences?,
        hasUserRating: Boolean,
        watched: Boolean,
    ): RecommendationResult {
        var score = 0.0
        val reasons = mutableListOf<String>()

        preferences?.contentTypes?.let {
            if (it.isEmpty() || it.contains(film.contentType)) {
                score += 2.0
                reasons += "Matches content preference"
            }
        }

        preferences?.weightedGenres?.forEach { (genre, weight) ->
            if (film.genres.any { it.equals(genre, ignoreCase = true) }) {
                score += weight
                reasons += "Matches genre $genre"
            }
        }

        preferences?.castAndDirectors?.forEach { favorite ->
            val found =
                film.cast.any { it.equals(favorite, ignoreCase = true) } ||
                    film.directors.any { it.equals(favorite, ignoreCase = true) }
            if (found) {
                score += 1.5
                reasons += "Matches favorite creator or cast member $favorite"
            }
        }

        preferences?.moods?.forEach { preferredMood ->
            if (mood != null && preferredMood.equals(mood, ignoreCase = true)) {
                score += 1.25
                reasons += "Matches requested mood $mood"
            }
        }

        film.imdbRating?.let {
            score += it / 2.0
            reasons += "Strong IMDb signal"
        }

        film.platformRating?.let {
            score += it
            reasons += "Strong platform signal"
        }

        if (hasUserRating) {
            score += 2.0
            reasons += "User has already rated similar content"
        }

        if (watched) {
            score -= 3.0
            reasons += "Already watched"
        }

        if (mood != null && film.title.contains(mood, ignoreCase = true)) {
            score += 0.5
        }

        if (reasons.isEmpty()) {
            reasons += "Baseline recommendation from library catalog"
        }

        return RecommendationResult(film = film, score = score, reasons = reasons)
    }
}
