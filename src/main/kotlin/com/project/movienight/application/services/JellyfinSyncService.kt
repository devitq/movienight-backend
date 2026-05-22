package com.project.movienight.application.services

import com.project.movienight.application.ports.input.JellyfinSyncUseCase
import com.project.movienight.application.ports.input.PushJellyfinCatalogCommand
import com.project.movienight.application.ports.output.BusinessMetricsPort
import com.project.movienight.application.ports.output.FilmLibraryEntryRepositoryPort
import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.application.ports.output.JellyfinSyncStateRepositoryPort
import com.project.movienight.application.ports.output.UserRepositoryPort
import com.project.movienight.domain.model.Film
import com.project.movienight.domain.model.FilmLibraryEntry
import com.project.movienight.domain.model.JellyfinSyncState
import com.project.movienight.domain.model.JellyfinSyncSummary
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@Service
class JellyfinSyncService(
    private val userRepository: UserRepositoryPort,
    private val filmRepository: FilmRepositoryPort,
    private val filmLibraryEntryRepository: FilmLibraryEntryRepositoryPort,
    private val syncStateRepository: JellyfinSyncStateRepositoryPort,
    private val idGenerator: IdGenerator,
    private val businessMetricsService: BusinessMetricsPort,
) : JellyfinSyncUseCase {
    override fun sync(command: PushJellyfinCatalogCommand): JellyfinSyncSummary =
        try {
            syncPushedCatalog(command)
        } catch (
            @Suppress("TooGenericExceptionCaught") ex: RuntimeException,
        ) {
            businessMetricsService.recordJellyfinSyncFailure()
            throw ex
        }

    override fun getSyncStates(): List<JellyfinSyncState> = syncStateRepository.findAll()

    private fun syncPushedCatalog(command: PushJellyfinCatalogCommand): JellyfinSyncSummary {
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
                filmRepository.findByJellyfinItemId(item.jellyfinItemId)?.copy(
                    title = item.title,
                    description = item.description.orEmpty(),
                    contentType = item.contentType,
                    releaseYear = item.releaseYear,
                    genres = item.genres,
                    cast = item.cast,
                    directors = item.directors,
                    imdbRating = item.imdbRating,
                    platformRating = item.platformRating,
                    externalUrl = item.externalUrl ?: item.imdbId?.let { imdbUrl(it) },
                    jellyfinItemId = item.jellyfinItemId,
                    jellyfinLibraryId = item.jellyfinLibraryId,
                ) ?: Film(
                    id = idGenerator.generateId(),
                    title = item.title,
                    description = item.description.orEmpty(),
                    contentType = item.contentType,
                    releaseYear = item.releaseYear,
                    genres = item.genres,
                    cast = item.cast,
                    directors = item.directors,
                    imdbRating = item.imdbRating,
                    platformRating = item.platformRating,
                    externalUrl = item.externalUrl ?: item.imdbId?.let { imdbUrl(it) },
                    jellyfinItemId = item.jellyfinItemId,
                    jellyfinLibraryId = item.jellyfinLibraryId,
                ).let { filmRepository.save(it) }

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
