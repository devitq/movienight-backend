package com.project.movienight.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "integrations.jellyfin")
data class JellyfinIntegrationProperties(
    val enabled: Boolean = false,
    val baseUrl: String = "",
    val webUrl: String = "",
    val apiKey: String = "",
    val syncIntervalMs: Long = 1_800_000,
    val requestTimeoutMs: Long = 20_000,
    val pluginToken: String = "",
)
