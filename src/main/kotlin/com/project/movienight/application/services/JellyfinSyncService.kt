package com.project.movienight.application.services

import com.project.movienight.application.ports.input.JellyfinPluginSyncCommand
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
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
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

    override fun syncFromPlugin(command: JellyfinPluginSyncCommand): JellyfinSyncSummary {
        val startedAt = Instant.now()
        val localUsersByJellyfinId =
            userRepository
                .findAll()
                .mapNotNull { user ->
                    user.jellyfinUserId?.let { it to user }
                }.toMap()
        val syncedItemCountsByUser = mutableMapOf<UUID, Int>()
        var skippedUsers = 0

        command.items.forEach { item ->
            val savedFilm =
                upsertFilm(
                    JellyfinLibraryItemSnapshot(
                        jellyfinItemId = item.jellyfinItemId,
                        title = item.title,
                        description = item.description.orEmpty(),
                        contentType = ContentType.FILM,
                        releaseYear = item.year,
                        genres = item.genres,
                        cast = emptyList(),
                        directors = emptyList(),
                        platformRating = null,
                        imdbRating = null,
                        externalUrl = item.imdbId?.let { imdbUrl(it) },
                        jellyfinLibraryId = null,
                        isPlayed = false,
                    ),
                )

            item.userStates.forEach { state ->
                val localUser = localUsersByJellyfinId[state.jellyfinUserId]
                if (localUser == null) {
                    skippedUsers += 1
                    return@forEach
                }

                syncedItemCountsByUser.merge(localUser.id, 1, Int::plus)
                if (state.isViewed) {
                    markFilmViewed(
                        userId = localUser.id,
                        filmId = savedFilm.id,
                        watchedAt = state.lastPlayedAt?.toLocalDateTime() ?: LocalDateTime.now(),
                    )
                }
            }
        }

        val now = LocalDateTime.now()
        syncedItemCountsByUser.forEach { (userId, syncedItemCount) ->
            syncStateRepository.save(
                JellyfinSyncState(
                    userId = userId,
                    lastSyncedAt = now,
                    lastSuccessfulSyncAt = now,
                    lastError = null,
                    syncedItemCount = syncedItemCount,
                ),
            )
        }

        val summary =
            JellyfinSyncSummary(
                syncedUsers = syncedItemCountsByUser.size,
                skippedUsers = skippedUsers,
                syncedItems = command.items.size,
                durationMs = Duration.between(startedAt, Instant.now()).toMillis(),
            )
        businessMetricsService.recordJellyfinSync(summary)
        return summary
    }

    private fun runSync(): JellyfinSyncSummary {
        val startedAt = Instant.now()
        val remoteUsers = jellyfinCatalog.fetchUsers()
        val localUsersByJellyfinId =
            userRepository
                .findAll()
                .mapNotNull { user ->
                    user.jellyfinUserId?.let { it to user }
                }.toMap()

        var syncedUsers = 0
        var skippedUsers = 0
        var syncedItems = 0

        remoteUsers.forEach { remoteUser ->
            val localUser = localUsersByJellyfinId[remoteUser.id]
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
        val film =
            filmRepository.findByJellyfinItemId(item.jellyfinItemId)?.copy(
                title = item.title,
                description = item.description,
                contentType = item.contentType,
                releaseYear = item.releaseYear,
                genres = item.genres,
                cast = item.cast,
                directors = item.directors,
                imdbRating = item.imdbRating,
                platformRating = item.platformRating,
                externalUrl = item.externalUrl,
                jellyfinItemId = item.jellyfinItemId,
                jellyfinLibraryId = item.jellyfinLibraryId,
            ) ?: Film(
                id = idGenerator.generateId(),
                title = item.title,
                description = item.description,
                contentType = item.contentType,
                releaseYear = item.releaseYear,
                genres = item.genres,
                cast = item.cast,
                directors = item.directors,
                imdbRating = item.imdbRating,
                platformRating = item.platformRating,
                externalUrl = item.externalUrl,
                jellyfinItemId = item.jellyfinItemId,
                jellyfinLibraryId = item.jellyfinLibraryId,
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

    private fun imdbUrl(imdbId: String): String {
        val normalized = imdbId.trim().lowercase().let { if (it.startsWith("tt")) it else "tt$it" }
        return "https://www.imdb.com/title/$normalized/"
    }
}
