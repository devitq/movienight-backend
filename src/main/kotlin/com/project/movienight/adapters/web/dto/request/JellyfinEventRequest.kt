package com.project.movienight.adapters.web.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

data class JellyfinEventRequest(
    @JsonProperty("event_id")
    val eventId: String,
    @JsonProperty("event_type")
    val eventType: String,
    @JsonProperty("occurred_at")
    val occurredAt: OffsetDateTime,
    @JsonProperty("jellyfin_user_id")
    val jellyfinUserId: String,
    @JsonProperty("item_id")
    val itemId: String,
    @JsonProperty("payload_version")
    val payloadVersion: Int = 1,
    @JsonProperty("payload")
    val payload: Map<String, Any>? = null,
)
