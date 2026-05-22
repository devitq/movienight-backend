package com.project.movienight.adapters.web

import com.project.movienight.config.JellyfinIntegrationProperties
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

fun requireJellyfinIntegrationEnabled(properties: JellyfinIntegrationProperties) {
    if (!properties.enabled) {
        throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Jellyfin integration is disabled")
    }
}

fun requireJellyfinPluginToken(
    properties: JellyfinIntegrationProperties,
    token: String?,
) {
    if (properties.pluginToken.isNotBlank() && token != properties.pluginToken) {
        throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid plugin token")
    }
}
