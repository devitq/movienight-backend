package com.project.movienight.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "integrations.jellyfin")
data class JellyfinIntegrationProperties(
    val enabled: Boolean = false,
    val webUrl: String = "",
    val pluginToken: String = "",
)
