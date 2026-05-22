package com.project.movienight.application.services

import com.project.movienight.application.ports.input.AcceptRecommendationCommand
import com.project.movienight.application.ports.input.AcceptRecommendationUseCase
import com.project.movienight.application.ports.input.GetRecommendationsUseCase
import com.project.movienight.application.ports.input.RecommendationQuery
import com.project.movienight.application.ports.input.RejectRecommendationCommand
import com.project.movienight.application.ports.input.RejectRecommendationUseCase
import com.project.movienight.application.ports.output.BusinessMetricsPort
import com.project.movienight.application.ports.output.FilmLibraryEntryRepositoryPort
import com.project.movienight.application.ports.output.FilmRatingRepositoryPort
import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.application.ports.output.RecommendationEventRepositoryPort
import com.project.movienight.application.ports.output.UserPreferencesRepositoryPort
import com.project.movienight.application.ports.output.UserRecommendationWeightsRepositoryPort
import com.project.movienight.application.ports.output.UserRepositoryPort
import com.project.movienight.domain.exception.EntityNotFoundException
import com.project.movienight.domain.model.Film
import com.project.movienight.domain.model.FilmLibraryEntry
import com.project.movienight.domain.model.FilmRating
import com.project.movienight.domain.model.RecommendationEvent
import com.project.movienight.domain.model.RecommendationEventType
import com.project.movienight.domain.model.RecommendationResult
import com.project.movienight.domain.model.UserPreferences
import com.project.movienight.domain.model.UserRecommendationWeights
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID
import kotlin.math.sqrt

@Service
class RecommendationService(
    private val filmRepository: FilmRepositoryPort,
    private val filmLibraryEntryRepository: FilmLibraryEntryRepositoryPort,
    private val filmRatingRepository: FilmRatingRepositoryPort,
    private val userPreferencesRepository: UserPreferencesRepositoryPort,
    private val userRepository: UserRepositoryPort,
    private val recommendationEventRepository: RecommendationEventRepositoryPort,
    private val userRecommendationWeightsRepository: UserRecommendationWeightsRepositoryPort,
    private val idGenerator: IdGenerator,
    private val businessMetricsService: BusinessMetricsPort,
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
        val libraryEntries = filmLibraryEntryRepository.findByUserId(query.userId)
        val libraryFilmIds = libraryEntries.map { it.filmId }.toSet()
        val watchedFilmIds = libraryEntries.filter { it.isViewed }.map { it.filmId }.toSet()
        val films = filmRepository.findAll()
        val filmsById = films.associateBy { it.id }
        val weights = findWeights(query.userId)
        val userProfile = buildUserProfile(preferences, ratings, libraryEntries, filmsById, weights)

        val candidates =
            films
                .asSequence()
                .filter { film -> query.contentType == null || film.contentType == query.contentType }
                .filter { film -> film.id !in watchedFilmIds }
                .filter { film -> !query.libraryOnly || film.id in libraryFilmIds }
                .toList()
        val scoredCandidates =
            candidates.map { film ->
                scoreFilm(film, query, preferences, userProfile, film.id in libraryFilmIds, weights)
            }
        val recommendationComparator =
            compareByDescending<ScoredRecommendation> { it.result.score }.thenBy {
                it.result.film.title
            }
        val scoredRecommendations =
            scoredCandidates
                .sortedWith(recommendationComparator)
                .take(query.limit.coerceAtLeast(1))

        scoredRecommendations.forEach { recommendation ->
            saveEvent(
                userId = query.userId,
                filmId = recommendation.result.film.id,
                eventType = RecommendationEventType.RECOMMENDED,
                score = recommendation.result.score,
                relevanceScore = recommendation.relevanceScore,
                qualityScore = recommendation.qualityScore,
                contextScore = recommendation.contextScore,
                noveltyScore = recommendation.noveltyScore,
                diversityScore = recommendation.diversityScore,
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
            scoredRecommendations.size,
        )
        if (log.isDebugEnabled) {
            log.debug(
                "Recommendation top results: userId='{}', results='{}'",
                query.userId,
                scoredRecommendations.joinToString(separator = ",") { "${it.result.film.id}:${it.result.score}" },
            )
        }

        return scoredRecommendations.map { it.result }
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

        val lastRecommendation = recommendationEventRepository.findLatestRecommended(userId, filmId)
        val event =
            saveEvent(
                userId = userId,
                filmId = filmId,
                eventType = eventType,
                score = lastRecommendation?.score,
                relevanceScore = lastRecommendation?.relevanceScore,
                qualityScore = lastRecommendation?.qualityScore,
                contextScore = lastRecommendation?.contextScore,
                noveltyScore = lastRecommendation?.noveltyScore,
                diversityScore = lastRecommendation?.diversityScore,
            )

        if (lastRecommendation != null) {
            updateRecommendationWeights(
                userId = userId,
                eventType = eventType,
                recommendation = lastRecommendation,
            )
        } else {
            log.info(
                "Recommendation feedback saved without weight update: userId='{}', filmId='{}', eventType='{}'",
                userId,
                filmId,
                eventType,
            )
        }

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
        relevanceScore: Double? = null,
        qualityScore: Double? = null,
        contextScore: Double? = null,
        noveltyScore: Double? = null,
        diversityScore: Double? = null,
    ): RecommendationEvent =
        recommendationEventRepository.save(
            RecommendationEvent(
                id = idGenerator.generateId(),
                userId = userId,
                filmId = filmId,
                eventType = eventType,
                score = score,
                relevanceScore = relevanceScore,
                qualityScore = qualityScore,
                contextScore = contextScore,
                noveltyScore = noveltyScore,
                diversityScore = diversityScore,
                createdAt = LocalDateTime.now(),
            ),
        )

    private fun findWeights(userId: UUID): UserRecommendationWeights =
        (
            userRecommendationWeightsRepository.findByUserId(userId)
                ?: UserRecommendationWeights.defaultFor(userId)
        ).normalized()

    private fun updateRecommendationWeights(
        userId: UUID,
        eventType: RecommendationEventType,
        recommendation: RecommendationEvent,
    ) {
        val current = findWeights(userId)
        val contributions = scoreContributions(recommendation, current) ?: return
        val direction =
            when (eventType) {
                RecommendationEventType.ACCEPTED -> 1.0
                RecommendationEventType.REJECTED -> -1.0
                RecommendationEventType.RECOMMENDED -> return
            }

        val updated =
            current
                .copy(
                    relevanceWeight = current.relevanceWeight + direction * LEARNING_RATE * contributions.relevance,
                    qualityWeight = current.qualityWeight + direction * LEARNING_RATE * contributions.quality,
                    contextWeight = current.contextWeight + direction * LEARNING_RATE * contributions.context,
                    noveltyWeight = current.noveltyWeight + direction * LEARNING_RATE * contributions.novelty,
                    diversityWeight = current.diversityWeight + direction * LEARNING_RATE * contributions.diversity,
                ).normalized(updatedAt = LocalDateTime.now())

        val saved = userRecommendationWeightsRepository.save(updated)
        businessMetricsService.recordRecommendationWeightsUpdated(eventType)
        log.info(
            RECOMMENDATION_WEIGHTS_UPDATED_LOG,
            userId,
            eventType,
            current.hashCode(),
            saved.hashCode(),
        )
    }

    private fun scoreContributions(
        recommendation: RecommendationEvent,
        weights: UserRecommendationWeights,
    ): ScoreContributions? {
        val rawContributions =
            listOf(
                weights.relevanceWeight to recommendation.relevanceScore,
                weights.qualityWeight to recommendation.qualityScore,
                weights.contextWeight to recommendation.contextScore,
                weights.noveltyWeight to recommendation.noveltyScore,
                weights.diversityWeight to recommendation.diversityScore,
            ).map { (weight, score) ->
                weight * (score?.takeIf { value -> value.isFinite() }?.coerceAtLeast(0.0) ?: 0.0)
            }
        val total = rawContributions.sum()
        if (total <= 0.0) {
            return null
        }
        return ScoreContributions(
            relevance = rawContributions[0] / total,
            quality = rawContributions[1] / total,
            context = rawContributions[2] / total,
            novelty = rawContributions[3] / total,
            diversity = rawContributions[4] / total,
        )
    }

    private fun buildUserProfile(
        preferences: UserPreferences?,
        ratings: List<FilmRating>,
        libraryEntries: List<FilmLibraryEntry>,
        filmsById: Map<UUID, Film>,
        weights: UserRecommendationWeights,
    ): UserTasteProfile {
        val preferenceProfile = MutableSparseVector()
        val positiveChoiceProfile = MutableSparseVector()
        val negativeChoiceProfile = MutableSparseVector()
        val libraryProfile = MutableSparseVector()

        preferences?.weightedGenres.orEmpty().forEach { (genre, weight) ->
            preferenceProfile.add(feature("genre", genre), weight.coerceAtLeast(1).toDouble() / MAX_PREFERENCE_WEIGHT)
        }
        preferences?.plotTypes.orEmpty().forEach { plotType ->
            tokenize(plotType).forEach { preferenceProfile.add(feature("plot", it), PREFERENCE_PLOT_WEIGHT) }
        }
        preferences?.eras.orEmpty().forEach { preferenceProfile.add(feature("era", it), PREFERENCE_ERA_WEIGHT) }
        preferences?.castAndDirectors.orEmpty().forEach {
            preferenceProfile.add(feature("person", it), PREFERENCE_PERSON_WEIGHT)
        }
        preferences?.moods.orEmpty().forEach { preferenceProfile.add(feature("mood", it), PREFERENCE_MOOD_WEIGHT) }
        preferences
            ?.contentTypes
            .orEmpty()
            .forEach {
                preferenceProfile.add(
                    feature("type", it.name),
                    PREFERENCE_CONTENT_TYPE_WEIGHT,
                )
            }

        ratings.forEach { rating ->
            val film = filmsById[rating.filmId] ?: return@forEach
            val signal = ratingSignal(rating.score)
            val filmVector = buildFilmVector(film, weights)
            when {
                signal >= POSITIVE_CHOICE_SIGNAL_THRESHOLD -> positiveChoiceProfile.add(filmVector.scale(signal))
                signal <= NEGATIVE_CHOICE_SIGNAL_THRESHOLD -> negativeChoiceProfile.add(filmVector.scale(-signal))
            }
        }

        libraryEntries.filterNot { it.isViewed }.forEach { entry ->
            val film = filmsById[entry.filmId] ?: return@forEach
            libraryProfile.add(buildFilmVector(film, weights).scale(LIBRARY_SIGNAL_WEIGHT))
        }

        val overallProfile = MutableSparseVector()
        overallProfile.add(preferenceProfile.toSparseVector())
        overallProfile.add(positiveChoiceProfile.toSparseVector().scale(EXPLICIT_CHOICE_PROFILE_WEIGHT))
        overallProfile.add(negativeChoiceProfile.toSparseVector().scale(-EXPLICIT_CHOICE_PROFILE_WEIGHT))
        overallProfile.add(libraryProfile.toSparseVector())

        return UserTasteProfile(
            overall = overallProfile.toSparseVector(),
            preferences = preferenceProfile.toSparseVector(),
            positiveChoices = positiveChoiceProfile.toSparseVector(),
            negativeChoices = negativeChoiceProfile.toSparseVector(),
            library = libraryProfile.toSparseVector(),
        )
    }

    private fun scoreFilm(
        film: Film,
        query: RecommendationQuery,
        preferences: UserPreferences?,
        userProfile: UserTasteProfile,
        inLibrary: Boolean,
        weights: UserRecommendationWeights,
    ): ScoredRecommendation {
        val reasons = mutableListOf<String>()
        val filmVector = buildFilmVector(film, weights)
        val relevanceBreakdown = relevanceScore(userProfile, filmVector)
        val preferenceScore = relevanceBreakdown.combined
        val qualityScore = qualityScore(film)
        val contextScore = contextScore(film, query, preferences, userProfile, preferenceScore)
        val noveltyScore = if (inLibrary) LIBRARY_NOVELTY_SCORE else CATALOG_NOVELTY_SCORE
        val diversityScore = diversityScore(film, preferences)
        val score =
            weights.relevanceWeight * preferenceScore +
                weights.qualityWeight * qualityScore +
                weights.contextWeight * contextScore +
                weights.noveltyWeight * noveltyScore +
                weights.diversityWeight * diversityScore

        if (relevanceBreakdown.positiveSimilarity > EXPLICIT_CHOICE_REASON_THRESHOLD) {
            reasons += "Similar to films you rated highly"
        } else if (preferenceScore > STRONG_REASON_THRESHOLD) {
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

        return ScoredRecommendation(
            result = RecommendationResult(film = film, score = roundScore(score), reasons = reasons.distinct()),
            relevanceScore = preferenceScore,
            qualityScore = qualityScore,
            contextScore = contextScore,
            noveltyScore = noveltyScore,
            diversityScore = diversityScore,
        )
    }

    private fun buildFilmVector(
        film: Film,
        weights: UserRecommendationWeights,
    ): SparseVector {
        val vector = MutableSparseVector()
        val normalizedGenres = film.genres.map(::normalize).filter { it.isNotBlank() }
        val plotTokens = tokenize("${film.title} ${film.description}")
        val moods = inferredMoods(film)
        val people = (film.directors + film.cast).map(::normalize).filter { it.isNotBlank() }

        vector.add(feature("type", film.contentType.name), weights.contentTypeVectorWeight)
        distribute(vector, "genre", normalizedGenres, weights.genreVectorWeight)
        distribute(vector, "plot", plotTokens, weights.plotVectorWeight)
        distribute(vector, "mood", moods, weights.moodVectorWeight)
        film.releaseYear?.let { vector.add(feature("era", decadeOf(it)), weights.eraVectorWeight) }
        distribute(vector, "person", people, weights.peopleVectorWeight)

        return vector.toSparseVector()
    }

    private fun relevanceScore(
        userProfile: UserTasteProfile,
        filmVector: SparseVector,
    ): RelevanceBreakdown {
        val overallSimilarity = cosineSimilarity(userProfile.overall, filmVector)
        val preferenceSimilarity = cosineSimilarity(userProfile.preferences, filmVector)
        val positiveSimilarity = cosineSimilarity(userProfile.positiveChoices, filmVector).coerceAtLeast(0.0)
        val negativeSimilarity = cosineSimilarity(userProfile.negativeChoices, filmVector).coerceAtLeast(0.0)
        val librarySimilarity = cosineSimilarity(userProfile.library, filmVector).coerceAtLeast(0.0)

        if (!userProfile.hasExplicitChoices) {
            return RelevanceBreakdown(
                combined = overallSimilarity,
                positiveSimilarity = positiveSimilarity,
            )
        }

        val positiveComponent =
            if (userProfile.hasPositiveChoices) {
                positiveSimilarity * POSITIVE_CHOICE_RELEVANCE_WEIGHT
            } else {
                0.0
            }
        val preferenceComponent = preferenceSimilarity.coerceAtLeast(0.0) * BROAD_PREFERENCE_RELEVANCE_WEIGHT
        val libraryComponent =
            if (userProfile.hasLibraryChoices) {
                librarySimilarity * LIBRARY_CHOICE_RELEVANCE_WEIGHT
            } else {
                0.0
            }
        val fallbackComponent = overallSimilarity.coerceAtLeast(0.0) * OVERALL_RELEVANCE_FALLBACK_WEIGHT
        val negativePenalty =
            if (userProfile.hasNegativeChoices) {
                negativeSimilarity * NEGATIVE_CHOICE_RELEVANCE_PENALTY
            } else {
                0.0
            }

        return RelevanceBreakdown(
            combined =
                (positiveComponent + preferenceComponent + libraryComponent + fallbackComponent - negativePenalty)
                    .coerceIn(MIN_RELEVANCE_SCORE, MAX_RELEVANCE_SCORE),
            positiveSimilarity = positiveSimilarity,
        )
    }

    private fun contextScore(
        film: Film,
        query: RecommendationQuery,
        preferences: UserPreferences?,
        userProfile: UserTasteProfile,
        relevanceScore: Double,
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

        val baseScore = if (checks == 0) BASE_CONTEXT_SCORE else score / checks
        if (!userProfile.hasExplicitChoices) {
            return baseScore
        }

        val relevanceGate =
            MIN_CONTEXT_RELEVANCE_GATE +
                (MAX_CONTEXT_RELEVANCE_GATE - MIN_CONTEXT_RELEVANCE_GATE) *
                relevanceScore.coerceIn(0.0, 1.0)
        return baseScore * relevanceGate
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
            filmGenres.none { it in preferredGenres } -> LOW_DIVERSITY_SCORE
            filmGenres.size > 1 -> HIGH_DIVERSITY_SCORE
            else -> MEDIUM_DIVERSITY_SCORE
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

    private data class ScoredRecommendation(
        val result: RecommendationResult,
        val relevanceScore: Double,
        val qualityScore: Double,
        val contextScore: Double,
        val noveltyScore: Double,
        val diversityScore: Double,
    )

    private data class RelevanceBreakdown(
        val combined: Double,
        val positiveSimilarity: Double,
    )

    private data class UserTasteProfile(
        val overall: SparseVector,
        val preferences: SparseVector,
        val positiveChoices: SparseVector,
        val negativeChoices: SparseVector,
        val library: SparseVector,
    ) {
        val hasPositiveChoices: Boolean = positiveChoices.values.isNotEmpty()
        val hasNegativeChoices: Boolean = negativeChoices.values.isNotEmpty()
        val hasLibraryChoices: Boolean = library.values.isNotEmpty()
        val hasExplicitChoices: Boolean = hasPositiveChoices || hasNegativeChoices || hasLibraryChoices
    }

    private data class ScoreContributions(
        val relevance: Double,
        val quality: Double,
        val context: Double,
        val novelty: Double,
        val diversity: Double,
    )

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
        private const val RECOMMENDATION_WEIGHTS_UPDATED_LOG =
            "Recommendation weights updated: userId='{}', eventType='{}', oldWeightsHash={}, newWeightsHash={}"

        private const val MAX_PREFERENCE_WEIGHT = 5.0
        private const val MAX_RATING_VALUE = 10.0
        private const val MIN_USER_RATING = 1
        private const val MAX_USER_RATING = 10
        private const val MIN_TOKEN_LENGTH = 3
        private const val MAX_REASON_ITEMS = 2
        private const val SCORE_ROUNDING_FACTOR = 1000.0

        private const val PREFERENCE_PLOT_WEIGHT = 0.6
        private const val PREFERENCE_ERA_WEIGHT = 0.7
        private const val PREFERENCE_PERSON_WEIGHT = 0.8
        private const val PREFERENCE_MOOD_WEIGHT = 0.8
        private const val PREFERENCE_CONTENT_TYPE_WEIGHT = 0.5
        private const val LIBRARY_SIGNAL_WEIGHT = 0.25
        private const val EXPLICIT_CHOICE_PROFILE_WEIGHT = 1.8

        private const val LEARNING_RATE = 0.03

        private const val LIBRARY_NOVELTY_SCORE = 0.85
        private const val CATALOG_NOVELTY_SCORE = 0.65
        private const val BASE_CONTEXT_SCORE = 0.5
        private const val BASE_QUALITY_SCORE = 0.5
        private const val BASE_DIVERSITY_SCORE = 0.5
        private const val HIGH_DIVERSITY_SCORE = 0.75
        private const val MEDIUM_DIVERSITY_SCORE = 0.45
        private const val LOW_DIVERSITY_SCORE = 0.15
        private const val STRONG_REASON_THRESHOLD = 0.15
        private const val EXPLICIT_CHOICE_REASON_THRESHOLD = 0.12
        private const val QUALITY_REASON_THRESHOLD = 0.75
        private const val POSITIVE_CHOICE_SIGNAL_THRESHOLD = 0.4
        private const val NEGATIVE_CHOICE_SIGNAL_THRESHOLD = -0.3
        private const val POSITIVE_CHOICE_RELEVANCE_WEIGHT = 0.78
        private const val BROAD_PREFERENCE_RELEVANCE_WEIGHT = 0.12
        private const val LIBRARY_CHOICE_RELEVANCE_WEIGHT = 0.08
        private const val OVERALL_RELEVANCE_FALLBACK_WEIGHT = 0.08
        private const val NEGATIVE_CHOICE_RELEVANCE_PENALTY = 0.65
        private const val MIN_RELEVANCE_SCORE = -1.0
        private const val MAX_RELEVANCE_SCORE = 1.0
        private const val MIN_CONTEXT_RELEVANCE_GATE = 0.35
        private const val MAX_CONTEXT_RELEVANCE_GATE = 1.0

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
