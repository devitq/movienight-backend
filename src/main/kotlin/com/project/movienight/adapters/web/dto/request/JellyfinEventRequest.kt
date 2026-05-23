package com.project.movienight.adapters.web.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import java.time.OffsetDateTime

data class JellyfinEventRequest(
    @JsonProperty("event_id")
    @field:NotBlank
    val eventId: String,
    @JsonProperty("event_type")
    @field:NotBlank
    val eventType: String,
    @JsonProperty("occurred_at")
    val occurredAt: OffsetDateTime,
    @JsonProperty("jellyfin_user_id")
    @field:NotBlank
    val jellyfinUserId: String,
    @JsonProperty("item_id")
    @field:NotBlank
    val itemId: String,
    @JsonProperty("payload_version")
    val payloadVersion: Int = 1,
    @JsonProperty("payload")
    val payload: Map<String, Any>? = null,
)
