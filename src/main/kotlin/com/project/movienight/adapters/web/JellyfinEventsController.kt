package com.project.movienight.adapters.web

import com.project.movienight.adapters.web.dto.request.JellyfinEventRequest
import com.project.movienight.application.ports.input.HandleJellyfinEventCommand
import com.project.movienight.application.ports.input.JellyfinEventUseCase
import com.project.movienight.config.JellyfinIntegrationProperties
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/integrations/jellyfin")
class JellyfinEventsController(
    private val jellyfinEventUseCase: JellyfinEventUseCase,
    private val properties: JellyfinIntegrationProperties,
) {
    private val log = LoggerFactory.getLogger(JellyfinEventsController::class.java)

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.OK)
    fun receiveEvent(
        @RequestHeader(value = "X-MovieNight-Plugin-Token", required = false) token: String?,
        @Valid @RequestBody request: JellyfinEventRequest,
    ) {
        if (!properties.enabled) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Jellyfin integration is disabled")
        }

        if (properties.pluginToken.isNotBlank()) {
            if (token == null || token != properties.pluginToken) {
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid plugin token")
            }
        }

        log.debug(
            "Received Jellyfin event {} for user {} item {}",
            request.eventId,
            request.jellyfinUserId,
            request.itemId,
        )
        jellyfinEventUseCase.handle(
            HandleJellyfinEventCommand(
                eventId = request.eventId,
                serverId = null,
                eventType = request.eventType,
                occurredAt = request.occurredAt,
                jellyfinUserId = request.jellyfinUserId,
                itemId = request.itemId,
                payload = request.payload,
            ),
        )
    }
}
