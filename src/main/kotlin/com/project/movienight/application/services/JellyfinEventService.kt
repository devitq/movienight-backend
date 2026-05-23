package com.project.movienight.application.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.movienight.application.ports.input.FilmLibraryUseCase
import com.project.movienight.application.ports.input.HandleJellyfinEventCommand
import com.project.movienight.application.ports.input.JellyfinEventUseCase
import com.project.movienight.application.ports.input.MarkFilmViewedCommand
import com.project.movienight.application.ports.output.BusinessMetricsPort
import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.application.ports.output.JellyfinEventRecord
import com.project.movienight.application.ports.output.JellyfinEventStorePort
import com.project.movienight.application.ports.output.UserRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class JellyfinEventService(
    private val jellyfinEventStore: JellyfinEventStorePort,
    private val userRepository: UserRepositoryPort,
    private val filmRepository: FilmRepositoryPort,
    private val filmLibraryUseCase: FilmLibraryUseCase,
    private val objectMapper: ObjectMapper,
    private val businessMetricsService: BusinessMetricsPort,
) : JellyfinEventUseCase {
    private val playbackEventTypes = setOf("playback.ended", "playback.stopped", "playback.completed")

    @Transactional
    override fun handle(command: HandleJellyfinEventCommand) {
        val jellyfinUserId = normalizeJellyfinId(command.jellyfinUserId)
        val jellyfinItemId = normalizeJellyfinId(command.itemId)
        val payloadJson = command.payload?.let { objectMapper.writeValueAsString(it) }
        val inserted =
            jellyfinEventStore.save(
                JellyfinEventRecord(
                    eventId = command.eventId,
                    serverId = command.serverId,
                    eventType = command.eventType,
                    occurredAt = command.occurredAt,
                    jellyfinUserId = jellyfinUserId,
                    jellyfinItemId = jellyfinItemId,
                    payload = payloadJson,
                ),
            )
        if (!inserted) {
            return
        }

        try {
            if (playbackEventTypes.contains(command.eventType)) {
                val localUser = userRepository.findByJellyfinUserId(jellyfinUserId)
                if (localUser == null) {
                    businessMetricsService.recordJellyfinUnmappedUser()
                    return
                }

                val film = filmRepository.findByJellyfinItemId(jellyfinItemId)
                if (film == null) {
                    businessMetricsService.recordBackendWriteFailure()
                    return
                }

                filmLibraryUseCase.markViewed(
                    MarkFilmViewedCommand(
                        userId = localUser.id,
                        filmId = film.id,
                        watchedAt = command.occurredAt.toLocalDateTime(),
                    ),
                )
            }
        } catch (
            @Suppress("TooGenericExceptionCaught") ex: RuntimeException,
        ) {
            businessMetricsService.recordBackendWriteFailure()
            throw ex
        }
    }

    private fun normalizeJellyfinId(value: String): String =
        runCatching { UUID.fromString(value).toString().replace("-", "") }
            .getOrDefault(value)
}
