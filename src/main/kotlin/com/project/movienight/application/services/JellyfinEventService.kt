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
        val payloadJson = command.payload?.let { objectMapper.writeValueAsString(it) }
        val inserted =
            jellyfinEventStore.save(
                JellyfinEventRecord(
                    eventId = command.eventId,
                    serverId = command.serverId,
                    eventType = command.eventType,
                    occurredAt = command.occurredAt,
                    jellyfinUserId = command.jellyfinUserId,
                    jellyfinItemId = command.itemId,
                    payload = payloadJson,
                ),
            )
        if (!inserted) {
            return
        }

        try {
            if (playbackEventTypes.contains(command.eventType)) {
                val localUser = userRepository.findByJellyfinUserId(command.jellyfinUserId)
                if (localUser == null) {
                    businessMetricsService.recordJellyfinUnmappedUser()
                    return
                }

                val film = filmRepository.findByJellyfinItemId(command.itemId)
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
}
