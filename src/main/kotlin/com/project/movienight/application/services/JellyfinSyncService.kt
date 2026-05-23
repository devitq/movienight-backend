package com.project.movienight.application.services

import com.project.movienight.application.ports.input.IngestJellyfinSyncCommand
import com.project.movienight.application.ports.input.JellyfinSyncUseCase
import com.project.movienight.application.ports.output.BusinessMetricsPort
import com.project.movienight.application.ports.output.FilmLibraryEntryRepositoryPort
import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.application.ports.output.JellyfinCatalogPort
import com.project.movienight.application.ports.output.JellyfinLibraryItemSnapshot
import com.project.movienight.application.ports.output.JellyfinSyncStateRepositoryPort
import com.project.movienight.application.ports.output.UserRepositoryPort
import com.project.movienight.config.JellyfinIntegrationProperties
import com.project.movienight.domain.model.ContentType
import com.project.movienight.domain.model.Film
import com.project.movienight.domain.model.FilmLibraryEntry
import com.project.movienight.domain.model.JellyfinSyncState
import com.project.movienight.domain.model.JellyfinSyncSummary
import com.project.movienight.domain.model.User
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID

@Service
class JellyfinSyncService(
    private val properties: JellyfinIntegrationProperties,
    private val jellyfinCatalog: JellyfinCatalogPort,
    private val userRepository: UserRepositoryPort,
    private val filmRepository: FilmRepositoryPort,
    private val filmLibraryEntryRepository: FilmLibraryEntryRepositoryPort,
    private val syncStateRepository: JellyfinSyncStateRepositoryPort,
    private val idGenerator: IdGenerator,
    private val businessMetricsService: BusinessMetricsPort,
) : JellyfinSyncUseCase {
    @Scheduled(fixedDelayString = "\${integrations.jellyfin.sync-interval-ms:1800000}")
    fun scheduledSync() {
        if (properties.enabled) {
            syncNow()
        }
    }

    override fun syncNow(): JellyfinSyncSummary {
        if (!properties.enabled || properties.baseUrl.isBlank() || properties.apiKey.isBlank()) {
            return JellyfinSyncSummary(syncedUsers = 0, skippedUsers = 0, syncedItems = 0, durationMs = 0)
        }

        return try {
            runSync()
        } catch (
            @Suppress("TooGenericExceptionCaught") ex: RuntimeException,
        ) {
            businessMetricsService.recordJellyfinSyncFailure()
            throw ex
        }
    }

    override fun getSyncStates(): List<JellyfinSyncState> = syncStateRepository.findAll()

    override fun ingest(command: IngestJellyfinSyncCommand): JellyfinSyncSummary {
        if (!properties.enabled) {
            return JellyfinSyncSummary(syncedUsers = 0, skippedUsers = 0, syncedItems = 0, durationMs = 0)
        }

        return try {
            ingestPluginSync(command)
        } catch (
            @Suppress("TooGenericExceptionCaught") ex: RuntimeException,
        ) {
            businessMetricsService.recordJellyfinSyncFailure()
            throw ex
        }
    }

    private fun runSync(): JellyfinSyncSummary {
        val startedAt = Instant.now()
        val remoteUsers = jellyfinCatalog.fetchUsers()
        val localUsersByJellyfinId =
            userRepository
                .findAll()
                .mapNotNull { user ->
                    user.jellyfinUserId?.let { normalizeJellyfinId(it) to user }
                }.toMap()

        var syncedUsers = 0
        var skippedUsers = 0
        var syncedItems = 0

        remoteUsers.forEach { remoteUser ->
            val localUser = localUsersByJellyfinId[normalizeJellyfinId(remoteUser.id)]
            if (localUser == null) {
                skippedUsers += 1
                return@forEach
            }

            val items = jellyfinCatalog.fetchLibraryItems(remoteUser.id)
            items.forEach { item ->
                syncItem(localUser.id, item)
                syncedItems += 1
            }

            val now = LocalDateTime.now()
            syncStateRepository.save(
                JellyfinSyncState(
                    userId = localUser.id,
                    lastSyncedAt = now,
                    lastSuccessfulSyncAt = now,
                    lastError = null,
                    syncedItemCount = items.size,
                ),
            )
            syncedUsers += 1
        }

        val summary =
            JellyfinSyncSummary(
                syncedUsers = syncedUsers,
                skippedUsers = skippedUsers,
                syncedItems = syncedItems,
                durationMs = Duration.between(startedAt, Instant.now()).toMillis(),
            )
        businessMetricsService.recordJellyfinSync(summary)
        return summary
    }

    private fun syncItem(
        userId: UUID,
        item: JellyfinLibraryItemSnapshot,
    ) {
        val savedFilm = upsertFilm(item)

        if (item.isPlayed) {
            markFilmViewed(
                userId = userId,
                filmId = savedFilm.id,
                watchedAt = LocalDateTime.now(),
            )
        }
    }

    private fun upsertFilm(item: JellyfinLibraryItemSnapshot): Film {
        val normalizedItem =
            item.copy(
                jellyfinItemId = normalizeJellyfinId(item.jellyfinItemId),
                jellyfinLibraryId = item.jellyfinLibraryId?.let(::normalizeJellyfinId),
            )
        val film =
            filmRepository.findByJellyfinItemId(normalizedItem.jellyfinItemId)?.copy(
                title = normalizedItem.title,
                description = normalizedItem.description,
                contentType = normalizedItem.contentType,
                releaseYear = normalizedItem.releaseYear,
                genres = normalizedItem.genres,
                cast = normalizedItem.cast,
                directors = normalizedItem.directors,
                imdbRating = normalizedItem.imdbRating,
                platformRating = normalizedItem.platformRating,
                externalUrl = normalizedItem.externalUrl,
                jellyfinItemId = normalizedItem.jellyfinItemId,
                jellyfinLibraryId = normalizedItem.jellyfinLibraryId,
            ) ?: Film(
                id = idGenerator.generateId(),
                title = normalizedItem.title,
                description = normalizedItem.description,
                contentType = normalizedItem.contentType,
                releaseYear = normalizedItem.releaseYear,
                genres = normalizedItem.genres,
                cast = normalizedItem.cast,
                directors = normalizedItem.directors,
                imdbRating = normalizedItem.imdbRating,
                platformRating = normalizedItem.platformRating,
                externalUrl = normalizedItem.externalUrl,
                jellyfinItemId = normalizedItem.jellyfinItemId,
                jellyfinLibraryId = normalizedItem.jellyfinLibraryId,
            )

        return filmRepository.save(film)
    }

    private fun markFilmViewed(
        userId: UUID,
        filmId: UUID,
        watchedAt: LocalDateTime,
    ) {
        val existingEntry = filmLibraryEntryRepository.findByUserIdAndFilmId(userId, filmId)
        filmLibraryEntryRepository.save(
            existingEntry?.copy(
                isViewed = true,
                watchedAt = watchedAt,
            ) ?: FilmLibraryEntry(
                id = idGenerator.generateId(),
                userId = userId,
                filmId = filmId,
                comment = null,
                isViewed = true,
                watchedAt = watchedAt,
            ),
        )
    }

    private fun ingestPluginSync(command: IngestJellyfinSyncCommand): JellyfinSyncSummary {
        val startedAt = Instant.now()
        upsertPluginUsers(command)
        val localUsersByJellyfinId =
            userRepository
                .findAll()
                .mapNotNull { user -> user.jellyfinUserId?.let { normalizeJellyfinId(it) to user } }
                .toMap()

        val skippedUserIds = mutableSetOf<String>()
        val syncedCountsByUserId = mutableMapOf<UUID, Int>()

        command.items.forEach { item ->
            val savedFilm =
                upsertFilm(
                    JellyfinLibraryItemSnapshot(
                        jellyfinItemId = item.jellyfinItemId,
                        title = item.title,
                        description = item.description ?: item.originalTitle ?: "",
                        contentType = ContentType.FILM,
                        releaseYear = item.year,
                        genres = item.genres,
                        cast = emptyList(),
                        directors = emptyList(),
                        platformRating = null,
                        imdbRating = null,
                        externalUrl = item.imdbId?.let { "https://www.imdb.com/title/$it/" },
                        jellyfinLibraryId = item.jellyfinLibraryId,
                        isPlayed = false,
                    ),
                )

            item.userStates.forEach { state ->
                val stateUserId = normalizeJellyfinId(state.jellyfinUserId)
                val localUser = localUsersByJellyfinId[stateUserId]
                if (localUser == null) {
                    skippedUserIds += stateUserId
                    return@forEach
                }

                syncedCountsByUserId[localUser.id] = syncedCountsByUserId.getOrDefault(localUser.id, 0) + 1
                if (state.isViewed || state.playCount > 0) {
                    markFilmViewed(
                        userId = localUser.id,
                        filmId = savedFilm.id,
                        watchedAt = state.lastPlayedAt?.toLocalDateTime() ?: LocalDateTime.now(),
                    )
                }
            }
        }

        val now = LocalDateTime.now()
        syncedCountsByUserId.forEach { (userId, itemCount) ->
            syncStateRepository.save(
                JellyfinSyncState(
                    userId = userId,
                    lastSyncedAt = now,
                    lastSuccessfulSyncAt = now,
                    lastError = null,
                    syncedItemCount = itemCount,
                ),
            )
        }

        val summary =
            JellyfinSyncSummary(
                syncedUsers = syncedCountsByUserId.size,
                skippedUsers = skippedUserIds.size,
                syncedItems = command.items.size,
                durationMs = Duration.between(startedAt, Instant.now()).toMillis(),
            )
        businessMetricsService.recordJellyfinSync(summary)
        return summary
    }

    private fun upsertPluginUsers(command: IngestJellyfinSyncCommand) {
        command.users.forEach { remoteUser ->
            val jellyfinUserId =
                remoteUser.jellyfinUserId
                    .takeIf { it.isNotBlank() }
                    ?.let(::normalizeJellyfinId)
                    ?: return@forEach
            if (userRepository.findByJellyfinUserId(jellyfinUserId) != null) {
                return@forEach
            }

            userRepository.save(
                User(
                    id = idGenerator.generateId(),
                    name = remoteUser.name?.takeIf { it.isNotBlank() } ?: "Jellyfin User",
                    email = syntheticJellyfinEmail(jellyfinUserId),
                    jellyfinUserId = jellyfinUserId,
                ),
            )
        }
    }

    private fun syntheticJellyfinEmail(jellyfinUserId: String): String {
        val safeId =
            jellyfinUserId
                .lowercase(Locale.getDefault())
                .replace(Regex("[^a-z0-9._%+-]"), "-")
                .take(240)
        return "jellyfin-$safeId@movienight.local"
    }

    private fun normalizeJellyfinId(value: String): String =
        runCatching { UUID.fromString(value).toString().replace("-", "") }
            .getOrDefault(value)
}
