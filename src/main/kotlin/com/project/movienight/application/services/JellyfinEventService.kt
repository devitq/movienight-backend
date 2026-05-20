package com.project.movienight.application.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.movienight.adapters.metrics.BusinessMetricsService
import com.project.movienight.adapters.persistence.jdbc.JellyfinEventRepository
import com.project.movienight.application.ports.input.MarkFilmViewedCommand
import com.project.movienight.application.ports.input.MarkFilmViewedUseCase
import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.application.ports.output.UserRepositoryPort
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class JellyfinEventService(
    private val jellyfinEventRepository: JellyfinEventRepository,
    private val userRepository: UserRepositoryPort,
    private val filmRepository: FilmRepositoryPort,
    private val markFilmViewedUseCase: MarkFilmViewedUseCase,
    private val objectMapper: ObjectMapper,
    private val businessMetricsService: BusinessMetricsService,
) {
    private val playbackEventTypes = setOf("playback.ended", "playback.stopped", "playback.completed")

    fun handleEvent(
        eventId: String,
        serverId: String?,
        eventType: String,
        occurredAt: OffsetDateTime,
        jellyfinUserId: String,
        itemId: String,
        payload: Map<String, Any>?,
    ) {
        val payloadJson = payload?.let { objectMapper.writeValueAsString(it) }
        val inserted =
            jellyfinEventRepository.save(
                eventId = eventId,
                serverId = serverId,
                eventType = eventType,
                occurredAt = occurredAt,
                jellyfinUserId = jellyfinUserId,
                jellyfinItemId = itemId,
                payload = payloadJson,
            )
        if (inserted != 1) {
            return
        }

        try {
            if (playbackEventTypes.contains(eventType)) {
                val localUser = userRepository.findAll().firstOrNull { it.jellyfinUserId == jellyfinUserId }
                if (localUser == null) {
                    jellyfinEventRepository.delete(eventId)
                    businessMetricsService.recordJellyfinUnmappedUser()
                    return
                }

                val film = filmRepository.findByJellyfinItemId(itemId)
                if (film == null) {
                    jellyfinEventRepository.delete(eventId)
                    businessMetricsService.recordBackendWriteFailure()
                    return
                }

                markFilmViewedUseCase.markViewed(
                    MarkFilmViewedCommand(
                        userId = localUser.id,
                        filmId = film.id,
                        watchedAt = occurredAt.toLocalDateTime(),
                    ),
                )
                businessMetricsService.recordLibraryEvent()
            }
        } catch (
            @Suppress("TooGenericExceptionCaught") ex: RuntimeException,
        ) {
            jellyfinEventRepository.delete(eventId)
            businessMetricsService.recordBackendWriteFailure()
            throw ex
        }
    }
}
