package com.project.movienight.application.ports.output

import java.time.OffsetDateTime

interface JellyfinEventRepositoryPort {
    fun save(
        eventId: String,
        serverId: String?,
        eventType: String,
        occurredAt: OffsetDateTime?,
        jellyfinUserId: String?,
        jellyfinItemId: String?,
        payload: String?,
    ): Int

    fun delete(eventId: String)
}