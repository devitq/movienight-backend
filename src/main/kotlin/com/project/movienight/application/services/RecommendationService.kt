package com.project.movienight.application.services

import com.project.movienight.adapters.metrics.BusinessMetricsService
import com.project.movienight.application.ports.input.AcceptRecommendationCommand
import com.project.movienight.application.ports.input.AcceptRecommendationUseCase
import com.project.movienight.application.ports.input.GetRecommendationsUseCase
import com.project.movienight.application.ports.input.RecommendationQuery
import com.project.movienight.application.ports.input.RejectRecommendationCommand
import com.project.movienight.application.ports.input.RejectRecommendationUseCase
import com.project.movienight.application.ports.output.FilmLibraryRepositoryPort
import com.project.movienight.application.ports.output.FilmRatingRepositoryPort
import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.application.ports.output.RecommendationEventRepositoryPort
import com.project.movienight.application.ports.output.UserPreferencesRepositoryPort
import com.project.movienight.application.ports.output.UserRepositoryPort
import com.project.movienight.domain.exception.EntityNotFoundException
import com.project.movienight.domain.model.Film
import com.project.movienight.domain.model.FilmLibrary
import com.project.movienight.domain.model.FilmRating
import com.project.movienight.domain.model.RecommendationEvent
import com.project.movienight.domain.model.RecommendationEventType
import com.project.movienight.domain.model.RecommendationResult
import com.project.movienight.domain.model.UserPreferences
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID
import kotlin.math.sqrt

@Service
class RecommendationService(
    private val filmRepository: FilmRepositoryPort,
    private val filmLibraryRepository: FilmLibraryRepositoryPort,
    private val filmRatingRepository: FilmRatingRepositoryPort,
    private val userPreferencesRepository: UserPreferencesRepositoryPort,
    private val userRepository: UserRepositoryPort,
    private val recommendationEventRepository: RecommendationEventRepositoryPort,
    private val idGenerator: IdGenerator,
    private val businessMetricsService: BusinessMetricsService,
) : GetRecommendationsUseCase,
    AcceptRecommendationUseCase,
    RejectRecommendationUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun recommend(query: RecommendationQuery): List<RecommendationResult> {
        businessMetricsService.recordRecommendationRequest()
        userRepository.findById(query.userId)
            ?: throw EntityNotFoundException(entity = "User", id = query.userId.toString())

        val preferences = userPreferencesRepository.findByUserId(query.userId)
        val ratings = filmRatingRepository.findByUserId(query.userId)
        val libraryEntries = filmLibraryRepository.findAll().filter { it.userId == query.userId }
        val libraryFilmIds = libraryEntries.map { it.filmId }.toSet()
        val watchedFilmIds = libraryEntries.filter { it.isViewed }.map { it.filmId }.toSet()
        val films = filmRepository.findAll()
        val filmsById = films.associateBy { it.id }
        val userProfile = buildUserProfile(preferences, ratings, libraryEntries, filmsById)

        val candidates =
            films
                .asSequence()
                .filter { film -> query.contentType == null || film.contentType == query.contentType }
                .filter { film -> film.id !in watchedFilmIds }
                .filter { film -> !query.libraryOnly || film.id in libraryFilmIds }
                .toList()
        val recommendations =
            candidates
                .asSequence()
                .map { film -> scoreFilm(film, query, preferences, userProfile, film.id in libraryFilmIds) }
                .sortedWith(compareByDescending<RecommendationResult> { it.score }.thenBy { it.film.title })
                .take(query.limit.coerceAtLeast(1))
                .toList()

        recommendations.forEach { recommendation ->
            saveEvent(
                userId = query.userId,
                filmId = recommendation.film.id,
                eventType = RecommendationEventType.RECOMMENDED,
                score = recommendation.score,
            )
        }

        log.info(
            RECOMMENDATION_COMPLETED_LOG,
            query.userId,
            query.contentType,
            !query.mood.isNullOrBlank(),
            query.libraryOnly,
            query.limit,
            candidates.size,
            recommendations.size,
        )
        if (log.isDebugEnabled) {
            log.debug(
                "Recommendation top results: userId='{}', results='{}'",
                query.userId,
                recommendations.joinToString(separator = ",") { "${it.film.id}:${it.score}" },
            )
        }

        return recommendations
    }

    override fun accept(command: AcceptRecommendationCommand): RecommendationEvent =
        saveFeedbackEvent(
            userId = command.userId,
            filmId = command.filmId,
            eventType = RecommendationEventType.ACCEPTED,
        )

    override fun reject(command: RejectRecommendationCommand): RecommendationEvent =
        saveFeedbackEvent(
            userId = command.userId,
            filmId = command.filmId,
            eventType = RecommendationEventType.REJECTED,
        )

    private fun saveFeedbackEvent(
        userId: UUID,
        filmId: UUID,
        eventType: RecommendationEventType,
    ): RecommendationEvent {
        userRepository.findById(userId)
            ?: throw EntityNotFoundException(entity = "User", id = userId.toString())
        filmRepository.findById(filmId)
            ?: throw EntityNotFoundException(entity = "Film", id = filmId.toString())

        val event =
            saveEvent(
                userId = userId,
                filmId = filmId,
                eventType = eventType,
                score = null,
            )

        log.info(
            RECOMMENDATION_FEEDBACK_SAVED_LOG,
            userId,
            filmId,
            eventType,
        )

        return event
    }

    private fun saveEvent(
        userId: UUID,
        filmId: UUID,
        eventType: RecommendationEventType,
        score: Double?,
    ): RecommendationEvent =
        recommendationEventRepository.save(
            RecommendationEvent(
                id = idGenerator.generateId(),
                userId = userId,
                filmId = filmId,
                eventType = eventType,
                score = score,
                createdAt = LocalDateTime.now(),
            ),
        )

    private fun buildUserProfile(
        preferences: UserPreferences?,
        ratings: List<FilmRating>,
        libraryEntries: List<FilmLibrary>,
        filmsById: Map<UUID, Film>,
    ): SparseVector {
        val profile = MutableSparseVector()

        preferences?.weightedGenres.orEmpty().forEach { (genre, weight) ->
            profile.add(feature("genre", genre), weight.coerceAtLeast(1).toDouble() / MAX_PREFERENCE_WEIGHT)
        }
        preferences?.plotTypes.orEmpty().forEach { plotType ->
            tokenize(plotType).forEach { profile.add(feature("plot", it), PREFERENCE_PLOT_WEIGHT) }
        }
        preferences?.eras.orEmpty().forEach { profile.add(feature("era", it), PREFERENCE_ERA_WEIGHT) }
        preferences?.castAndDirectors.orEmpty().forEach { profile.add(feature("person", it), PREFERENCE_PERSON_WEIGHT) }
        preferences?.moods.orEmpty().forEach { profile.add(feature("mood", it), PREFERENCE_MOOD_WEIGHT) }
        preferences
            ?.contentTypes
            .orEmpty()
            .forEach {
                profile.add(
                    feature("type", it.name),
                    PREFERENCE_CONTENT_TYPE_WEIGHT,
                )
            }

        ratings.forEach { rating ->
            val film = filmsById[rating.filmId] ?: return@forEach
            val signal = ratingSignal(rating.score)
            profile.add(buildFilmVector(film).scale(signal))
        }

        libraryEntries.filterNot { it.isViewed }.forEach { entry ->
            val film = filmsById[entry.filmId] ?: return@forEach
            profile.add(buildFilmVector(film).scale(LIBRARY_SIGNAL_WEIGHT))
        }

        return profile.toSparseVector()
    }

    private fun scoreFilm(
        film: Film,
        query: RecommendationQuery,
        preferences: UserPreferences?,
        userProfile: SparseVector,
        inLibrary: Boolean,
    ): RecommendationResult {
        val reasons = mutableListOf<String>()
        val filmVector = buildFilmVector(film)
        val preferenceScore = cosineSimilarity(userProfile, filmVector)
        val qualityScore = qualityScore(film)
        val contextScore = contextScore(film, query, preferences)
        val noveltyScore = if (inLibrary) LIBRARY_NOVELTY_SCORE else CATALOG_NOVELTY_SCORE
        val diversityScore = diversityScore(film, preferences)
        val score =
            RELEVANCE_WEIGHT * preferenceScore +
                QUALITY_WEIGHT * qualityScore +
                CONTEXT_WEIGHT * contextScore +
                NOVELTY_WEIGHT * noveltyScore +
                DIVERSITY_WEIGHT * diversityScore

        if (preferenceScore > STRONG_REASON_THRESHOLD) {
            reasons += "Similar to user preferences and rating history"
        }
        matchingGenres(film, preferences).take(MAX_REASON_ITEMS).forEach { genre ->
            reasons += "Matches preferred genre: $genre"
        }
        matchingPeople(film, preferences).take(MAX_REASON_ITEMS).forEach { person ->
            reasons += "Matches preferred cast or director: $person"
        }
        query.mood?.takeIf { inferredMoods(film).contains(normalize(it)) }?.let { mood ->
            reasons += "Matches requested mood: $mood"
        }
        film.releaseYear?.let { year ->
            if (preferences?.eras.orEmpty().any { normalize(it) == normalize(decadeOf(year)) }) {
                reasons += "Matches preferred era: ${decadeOf(year)}"
            }
        }
        if (qualityScore >= QUALITY_REASON_THRESHOLD) {
            reasons += "High rating signal"
        }
        if (inLibrary) {
            reasons += "Already in user library"
        }

        if (reasons.isEmpty()) {
            reasons += "Baseline recommendation from catalog quality"
        }

        return RecommendationResult(film = film, score = roundScore(score), reasons = reasons.distinct())
    }

    private fun buildFilmVector(film: Film): SparseVector {
        val vector = MutableSparseVector()
        val normalizedGenres = film.genres.map(::normalize).filter { it.isNotBlank() }
        val plotTokens = tokenize("${film.title} ${film.description}")
        val moods = inferredMoods(film)
        val people = (film.directors + film.cast).map(::normalize).filter { it.isNotBlank() }

        vector.add(feature("type", film.contentType.name), CONTENT_TYPE_VECTOR_WEIGHT)
        distribute(vector, "genre", normalizedGenres, GENRE_VECTOR_WEIGHT)
        distribute(vector, "plot", plotTokens, PLOT_VECTOR_WEIGHT)
        distribute(vector, "mood", moods, MOOD_VECTOR_WEIGHT)
        film.releaseYear?.let { vector.add(feature("era", decadeOf(it)), ERA_VECTOR_WEIGHT) }
        distribute(vector, "person", people, PEOPLE_VECTOR_WEIGHT)

        return vector.toSparseVector()
    }

    private fun contextScore(
        film: Film,
        query: RecommendationQuery,
        preferences: UserPreferences?,
    ): Double {
        var score = 0.0
        var checks = 0

        query.mood?.let {
            checks += 1
            if (inferredMoods(film).contains(normalize(it))) {
                score += 1.0
            }
        }
        preferences?.contentTypes?.takeIf { it.isNotEmpty() }?.let {
            checks += 1
            if (film.contentType in it) {
                score += 1.0
            }
        }
        preferences?.eras?.takeIf { it.isNotEmpty() }?.let { eras ->
            film.releaseYear?.let {
                checks += 1
                if (eras.any { era -> normalize(era) == normalize(decadeOf(it)) }) {
                    score += 1.0
                }
            }
        }

        return if (checks == 0) BASE_CONTEXT_SCORE else score / checks
    }

    private fun qualityScore(film: Film): Double {
        val normalizedRatings =
            listOfNotNull(
                film.imdbRating?.let { normalizeRating(it) },
                film.platformRating?.let { normalizeRating(it) },
            )
        return normalizedRatings.averageOrNull() ?: BASE_QUALITY_SCORE
    }

    private fun diversityScore(
        film: Film,
        preferences: UserPreferences?,
    ): Double {
        val preferredGenres =
            preferences
                ?.weightedGenres
                .orEmpty()
                .keys
                .map(::normalize)
                .toSet()
        val filmGenres = film.genres.map(::normalize).toSet()
        return when {
            preferredGenres.isEmpty() -> BASE_DIVERSITY_SCORE
            filmGenres.none { it in preferredGenres } -> HIGH_DIVERSITY_SCORE
            filmGenres.size > 1 -> MEDIUM_DIVERSITY_SCORE
            else -> LOW_DIVERSITY_SCORE
        }
    }

    private fun inferredMoods(film: Film): Set<String> {
        val text = normalize("${film.title} ${film.description} ${film.genres.joinToString(" ")}")
        return moodLexicon
            .filterValues { keywords -> keywords.any { keyword -> text.contains(keyword) } }
            .keys
    }

    private fun matchingGenres(
        film: Film,
        preferences: UserPreferences?,
    ): List<String> {
        val filmGenres = film.genres.associateBy { normalize(it) }
        return preferences
            ?.weightedGenres
            .orEmpty()
            .keys
            .map(::normalize)
            .mapNotNull { filmGenres[it] }
    }

    private fun matchingPeople(
        film: Film,
        preferences: UserPreferences?,
    ): List<String> {
        val people = (film.cast + film.directors).associateBy { normalize(it) }
        return preferences
            ?.castAndDirectors
            .orEmpty()
            .map(::normalize)
            .mapNotNull { people[it] }
    }

    private fun distribute(
        vector: MutableSparseVector,
        namespace: String,
        values: Collection<String>,
        totalWeight: Double,
    ) {
        val uniqueValues = values.map(::normalize).filter { it.isNotBlank() }.distinct()
        if (uniqueValues.isEmpty()) {
            return
        }
        val itemWeight = totalWeight / uniqueValues.size
        uniqueValues.forEach { vector.add(feature(namespace, it), itemWeight) }
    }

    private fun ratingSignal(score: Int): Double =
        when (score.coerceIn(MIN_USER_RATING, MAX_USER_RATING)) {
            10 -> 1.0
            9 -> 0.9
            8 -> 0.7
            7 -> 0.4
            6 -> 0.1
            5 -> 0.0
            4 -> -0.3
            3 -> -0.5
            2 -> -0.8
            else -> -1.0
        }

    private fun normalizeRating(rating: Double): Double = (rating / MAX_RATING_VALUE).coerceIn(0.0, 1.0)

    private fun decadeOf(year: Int): String = "${year / 10 * 10}s"

    private fun tokenize(text: String): List<String> =
        normalize(text)
            .split(tokenSeparatorRegex)
            .asSequence()
            .filter { it.length >= MIN_TOKEN_LENGTH }
            .filterNot { it in stopWords }
            .distinct()
            .toList()

    private fun feature(
        namespace: String,
        value: String,
    ): String = "$namespace:${normalize(value)}"

    private fun normalize(value: String): String =
        value
            .trim()
            .lowercase(Locale.getDefault())

    private fun cosineSimilarity(
        left: SparseVector,
        right: SparseVector,
    ): Double {
        if (left.values.isEmpty() || right.values.isEmpty()) {
            return 0.0
        }

        val dot =
            left.values
                .entries
                .sumOf { (feature, weight) -> weight * (right.values[feature] ?: 0.0) }
        val leftNorm = sqrt(left.values.values.sumOf { it * it })
        val rightNorm = sqrt(right.values.values.sumOf { it * it })
        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0
        }

        return dot / (leftNorm * rightNorm)
    }

    private fun roundScore(score: Double): Double =
        kotlin.math.round(score * SCORE_ROUNDING_FACTOR) / SCORE_ROUNDING_FACTOR

    private fun Iterable<Double>.averageOrNull(): Double? {
        val values = toList()
        return values.takeIf { it.isNotEmpty() }?.average()
    }

    private data class SparseVector(
        val values: Map<String, Double>,
    ) {
        fun scale(weight: Double): SparseVector = SparseVector(values.mapValues { it.value * weight })
    }

    private class MutableSparseVector {
        private val values = mutableMapOf<String, Double>()

        fun add(
            feature: String,
            weight: Double,
        ) {
            if (weight == 0.0) {
                return
            }
            values[feature] = (values[feature] ?: 0.0) + weight
        }

        fun add(vector: SparseVector) {
            vector.values.forEach { (feature, weight) -> add(feature, weight) }
        }

        fun toSparseVector(): SparseVector = SparseVector(values.filterValues { it != 0.0 })
    }

    private companion object {
        private const val RECOMMENDATION_COMPLETED_LOG =
            "Recommendation request completed: userId='{}', contentType='{}', moodPresent={}, " +
                "libraryOnly={}, limit={}, candidatesCount={}, returnedCount={}"
        private const val RECOMMENDATION_FEEDBACK_SAVED_LOG =
            "Recommendation feedback saved: userId='{}', filmId='{}', eventType='{}'"

        private const val MAX_PREFERENCE_WEIGHT = 5.0
        private const val MAX_RATING_VALUE = 10.0
        private const val MIN_USER_RATING = 1
        private const val MAX_USER_RATING = 10
        private const val MIN_TOKEN_LENGTH = 3
        private const val MAX_REASON_ITEMS = 2
        private const val SCORE_ROUNDING_FACTOR = 1000.0

        private const val CONTENT_TYPE_VECTOR_WEIGHT = 0.05
        private const val GENRE_VECTOR_WEIGHT = 0.25
        private const val PLOT_VECTOR_WEIGHT = 0.35
        private const val MOOD_VECTOR_WEIGHT = 0.15
        private const val ERA_VECTOR_WEIGHT = 0.10
        private const val PEOPLE_VECTOR_WEIGHT = 0.10

        private const val PREFERENCE_PLOT_WEIGHT = 0.6
        private const val PREFERENCE_ERA_WEIGHT = 0.7
        private const val PREFERENCE_PERSON_WEIGHT = 0.8
        private const val PREFERENCE_MOOD_WEIGHT = 0.8
        private const val PREFERENCE_CONTENT_TYPE_WEIGHT = 0.5
        private const val LIBRARY_SIGNAL_WEIGHT = 0.25

        private const val RELEVANCE_WEIGHT = 0.55
        private const val QUALITY_WEIGHT = 0.15
        private const val CONTEXT_WEIGHT = 0.10
        private const val NOVELTY_WEIGHT = 0.10
        private const val DIVERSITY_WEIGHT = 0.10

        private const val LIBRARY_NOVELTY_SCORE = 0.85
        private const val CATALOG_NOVELTY_SCORE = 0.65
        private const val BASE_CONTEXT_SCORE = 0.5
        private const val BASE_QUALITY_SCORE = 0.5
        private const val BASE_DIVERSITY_SCORE = 0.5
        private const val HIGH_DIVERSITY_SCORE = 1.0
        private const val MEDIUM_DIVERSITY_SCORE = 0.6
        private const val LOW_DIVERSITY_SCORE = 0.3
        private const val STRONG_REASON_THRESHOLD = 0.15
        private const val QUALITY_REASON_THRESHOLD = 0.75

        private val tokenSeparatorRegex = Regex("[^\\p{L}0-9]+")
        private val stopWords =
            setOf(
                "and",
                "the",
                "for",
                "with",
                "about",
                "into",
                "from",
            )
        private val moodLexicon =
            mapOf(
                "tense" to listOf("thriller", "suspense", "tension", "rescue", "crime"),
                "slow-burn" to listOf("slow", "meditative", "grounded"),
                "feel-good" to listOf("comedy", "family", "summer", "kind", "warm"),
                "dark" to listOf("dark", "noir", "horror", "murder", "crime"),
                "romantic" to listOf("romance", "love", "relationship"),
                "focused" to listOf("science", "mission", "detective", "investigation", "sci-fi"),
            )
    }
}
