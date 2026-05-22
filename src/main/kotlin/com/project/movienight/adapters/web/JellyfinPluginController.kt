package com.project.movienight.adapters.web

import com.project.movienight.adapters.web.dto.request.RateFilmRequest
import com.project.movienight.adapters.web.dto.request.RecommendationOnboardingRequest
import com.project.movienight.adapters.web.dto.response.FilmLibraryEntryResponse
import com.project.movienight.adapters.web.dto.response.FilmRatingResponse
import com.project.movienight.adapters.web.dto.response.RecommendationOnboardingResponse
import com.project.movienight.adapters.web.dto.response.RecommendationResponse
import com.project.movienight.adapters.web.dto.response.UserPreferencesResponse
import com.project.movienight.application.ports.input.CompleteRecommendationOnboardingCommand
import com.project.movienight.application.ports.input.CompleteRecommendationOnboardingUseCase
import com.project.movienight.application.ports.input.FilmLibraryUseCase
import com.project.movienight.application.ports.input.FilmRatingUseCase
import com.project.movienight.application.ports.input.GetRecommendationsUseCase
import com.project.movienight.application.ports.input.MarkFilmViewedCommand
import com.project.movienight.application.ports.input.RateFilmCommand
import com.project.movienight.application.ports.input.RecommendationQuery
import com.project.movienight.application.ports.input.UserPreferencesUseCase
import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.application.ports.output.UserRepositoryPort
import com.project.movienight.config.JellyfinIntegrationProperties
import com.project.movienight.domain.exception.EntityNotFoundException
import com.project.movienight.domain.model.Film
import com.project.movienight.domain.model.RecommendationStyle
import com.project.movienight.domain.model.User
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.Locale
import java.util.UUID

@RestController
@RequestMapping("/api/integrations/jellyfin")
class JellyfinPluginController(
    private val authenticator: JellyfinPluginAuthenticator,
    private val userRepository: UserRepositoryPort,
    private val filmRepository: FilmRepositoryPort,
    private val idGenerator: IdGenerator,
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val filmRatingUseCase: FilmRatingUseCase,
    private val filmLibraryUseCase: FilmLibraryUseCase,
    private val userPreferencesUseCase: UserPreferencesUseCase,
    private val completeRecommendationOnboardingUseCase: CompleteRecommendationOnboardingUseCase,
    private val jellyfinProperties: JellyfinIntegrationProperties,
) {
    @GetMapping("/users/{jellyfinUserId}/recommendations")
    fun recommend(
        @RequestHeader(value = "X-MovieNight-Plugin-Token", required = false) token: String?,
        @PathVariable jellyfinUserId: String,
        @RequestParam(required = false) contentType: String?,
        @RequestParam(required = false) mood: String?,
        @RequestParam(required = false, defaultValue = "false") libraryOnly: Boolean,
        @RequestParam(required = false, defaultValue = "10") limit: Int,
    ): List<RecommendationResponse> {
        authenticator.authenticate(token)
        val user = resolveOrCreateUser(jellyfinUserId)
        return getRecommendationsUseCase
            .recommend(
                RecommendationQuery(
                    userId = user.id,
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
    }

    @PostMapping("/users/{jellyfinUserId}/ratings/items/{jellyfinItemId}")
    fun rate(
        @RequestHeader(value = "X-MovieNight-Plugin-Token", required = false) token: String?,
        @PathVariable jellyfinUserId: String,
        @PathVariable jellyfinItemId: String,
        @Valid @RequestBody request: RateFilmRequest,
    ): FilmRatingResponse {
        authenticator.authenticate(token)
        val user = resolveOrCreateUser(jellyfinUserId)
        val film = resolveFilm(jellyfinItemId)
        return FilmRatingResponse.fromDomain(
            filmRatingUseCase.rate(
                RateFilmCommand(
                    userId = user.id,
                    filmId = film.id,
                    score = request.score,
                    note = request.note,
                ),
            ),
        )
    }

    @GetMapping("/users/{jellyfinUserId}/ratings")
    fun ratings(
        @RequestHeader(value = "X-MovieNight-Plugin-Token", required = false) token: String?,
        @PathVariable jellyfinUserId: String,
    ): List<FilmRatingResponse> {
        authenticator.authenticate(token)
        val user = resolveOrCreateUser(jellyfinUserId)
        return filmRatingUseCase.getRatings(user.id).map { FilmRatingResponse.fromDomain(it) }
    }

    @PostMapping("/users/{jellyfinUserId}/library/items/{jellyfinItemId}/viewed")
    fun markViewed(
        @RequestHeader(value = "X-MovieNight-Plugin-Token", required = false) token: String?,
        @PathVariable jellyfinUserId: String,
        @PathVariable jellyfinItemId: String,
        @RequestBody(required = false) request: JellyfinViewedRequest?,
    ): FilmLibraryEntryResponse {
        authenticator.authenticate(token)
        val user = resolveOrCreateUser(jellyfinUserId)
        val film = resolveFilm(jellyfinItemId)
        return FilmLibraryEntryResponse.fromDomain(
            filmLibraryUseCase.markViewed(
                MarkFilmViewedCommand(
                    userId = user.id,
                    filmId = film.id,
                    watchedAt = request?.watchedAt?.toLocalDateTime(),
                ),
            ),
        )
    }

    @GetMapping("/users/{jellyfinUserId}/preferences")
    fun preferences(
        @RequestHeader(value = "X-MovieNight-Plugin-Token", required = false) token: String?,
        @PathVariable jellyfinUserId: String,
    ): UserPreferencesResponse {
        authenticator.authenticate(token)
        val user = resolveOrCreateUser(jellyfinUserId)
        return userPreferencesUseCase.get(user.id)?.let { UserPreferencesResponse.fromDomain(it) }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User preferences not found")
    }

    @PostMapping("/users/{jellyfinUserId}/recommendation-onboarding")
    fun completeOnboarding(
        @RequestHeader(value = "X-MovieNight-Plugin-Token", required = false) token: String?,
        @PathVariable jellyfinUserId: String,
        @RequestBody request: RecommendationOnboardingRequest,
    ): RecommendationOnboardingResponse {
        authenticator.authenticate(token)
        val user = resolveOrCreateUser(jellyfinUserId)
        return RecommendationOnboardingResponse.fromApplication(
            completeRecommendationOnboardingUseCase.complete(
                CompleteRecommendationOnboardingCommand(
                    userId = user.id,
                    weightedGenres = request.weightedGenres,
                    plotTypes = request.plotTypes,
                    eras = request.eras,
                    castAndDirectors = request.castAndDirectors,
                    moods = request.moods,
                    contentTypes = request.contentTypes.mapNotNull { runCatching { parseContentType(it) }.getOrNull() },
                    likedFilmIds = request.likedFilmIds,
                    dislikedFilmIds = request.dislikedFilmIds,
                    libraryFilmIds = request.libraryFilmIds,
                    watchedFilmIds = request.watchedFilmIds,
                    recommendationStyle = parseRecommendationStyle(request.recommendationStyle),
                ),
            ),
        )
    }

    private fun resolveOrCreateUser(jellyfinUserId: String): User =
        normalizeJellyfinId(jellyfinUserId).let { normalizedId ->
            userRepository.findByJellyfinUserId(normalizedId)
                ?: userRepository.save(
                    User(
                        id = idGenerator.generateId(),
                        name = "Jellyfin User",
                        email = syntheticJellyfinEmail(normalizedId),
                        jellyfinUserId = normalizedId,
                    ),
                )
        }

    private fun resolveFilm(jellyfinItemId: String): Film =
        normalizeJellyfinId(jellyfinItemId).let { normalizedId ->
            filmRepository.findByJellyfinItemId(normalizedId)
                ?: throw EntityNotFoundException(entity = "Jellyfin item", id = jellyfinItemId)
        }

    private fun buildWatchUrl(jellyfinItemId: String?): String? {
        if (jellyfinItemId.isNullOrBlank() || jellyfinProperties.webUrl.isBlank()) {
            return null
        }

        val baseUrl = jellyfinProperties.webUrl.trimEnd('/')
        val encodedItemId = URLEncoder.encode(jellyfinItemId, StandardCharsets.UTF_8)
        return "$baseUrl/web/#/details?id=$encodedItemId"
    }

    private fun parseRecommendationStyle(value: String): RecommendationStyle =
        runCatching { RecommendationStyle.valueOf(value.uppercase()) }
            .getOrDefault(RecommendationStyle.BALANCED)

    private fun normalizeJellyfinId(value: String): String =
        runCatching { UUID.fromString(value).toString().replace("-", "") }
            .getOrDefault(value)

    private fun syntheticJellyfinEmail(jellyfinUserId: String): String {
        val safeId =
            jellyfinUserId
                .lowercase(Locale.getDefault())
                .replace(Regex("[^a-z0-9._%+-]"), "-")
                .take(240)
        return "jellyfin-$safeId@movienight.local"
    }
}

data class JellyfinViewedRequest(
    val watchedAt: OffsetDateTime? = null,
)
