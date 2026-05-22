package com.project.movienight.application.services

import com.project.movienight.adapters.jellyfin.JellyfinApiClient
import com.project.movienight.adapters.jellyfin.JellyfinLibraryItemSnapshot
import com.project.movienight.adapters.jellyfin.JellyfinRemoteUser
import com.project.movienight.adapters.metrics.BusinessMetricsService
import com.project.movienight.application.ports.output.FilmLibraryRepositoryPort
import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.application.ports.output.JellyfinSyncStateRepositoryPort
import com.project.movienight.application.ports.output.UserRepositoryPort
import com.project.movienight.config.JellyfinIntegrationProperties
import com.project.movienight.domain.model.ContentType
import com.project.movienight.domain.model.Film
import com.project.movienight.domain.model.FilmLibrary
import com.project.movienight.domain.model.JellyfinSyncState
import com.project.movienight.domain.model.JellyfinSyncSummary
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

@Service
class JellyfinSyncService(
    private val properties: JellyfinIntegrationProperties,
    private val jellyfinApiClient: JellyfinApiClient,
    private val userRepository: UserRepositoryPort,
    private val filmRepository: FilmRepositoryPort,
    private val filmLibraryRepository: FilmLibraryRepositoryPort,
    private val syncStateRepository: JellyfinSyncStateRepositoryPort,
    private val idGenerator: IdGenerator,
    private val businessMetricsService: BusinessMetricsService,
) {
    @Scheduled(fixedDelayString = "\${integrations.jellyfin.sync-interval-ms:1800000}")
    fun scheduledSync() {
        if (properties.enabled) {
            syncNow()
        }
    }

    fun syncNow(): JellyfinSyncSummary {
        if (!properties.enabled || properties.baseUrl.isBlank() || properties.apiKey.isBlank()) {
            return JellyfinSyncSummary(syncedUsers = 0, skippedUsers = 0, syncedItems = 0, durationMs = 0)
        }

        val startedAt = Instant.now()
        val remoteUsers = jellyfinApiClient.fetchUsers()
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

            val items = jellyfinApiClient.fetchLibraryItems(remoteUser.id)
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

    fun getSyncStates(): List<JellyfinSyncState> = syncStateRepository.findAll()

    private fun syncItem(
        userId: java.util.UUID,
        item: JellyfinLibraryItemSnapshot,
    ) {
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

        val savedFilm = filmRepository.save(film)

        if (item.isPlayed) {
            val watchedAt = LocalDateTime.now()
            val existingEntry = filmLibraryRepository.findByUserIdAndFilmId(userId, savedFilm.id)
            filmLibraryRepository.save(
                existingEntry?.copy(
                    isViewed = true,
                    watchedAt = watchedAt,
                ) ?: FilmLibrary(
                    id = idGenerator.generateId(),
                    userId = userId,
                    filmId = savedFilm.id,
                    comment = null,
                    isViewed = true,
                    watchedAt = watchedAt,
                ),
            )
        }
    }
}
