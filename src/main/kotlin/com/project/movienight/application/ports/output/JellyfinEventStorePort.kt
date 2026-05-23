package com.project.movienight.application.ports.output

import java.time.OffsetDateTime

interface JellyfinEventStorePort {
    fun save(event: JellyfinEventRecord): Boolean
}

data class JellyfinEventRecord(
    val eventId: String,
    val serverId: String?,
    val eventType: String,
    val occurredAt: OffsetDateTime?,
    val jellyfinUserId: String?,
    val jellyfinItemId: String?,
    val payload: String?,
)
