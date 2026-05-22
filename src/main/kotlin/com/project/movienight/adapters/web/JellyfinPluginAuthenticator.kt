package com.project.movienight.adapters.web

import com.project.movienight.config.JellyfinIntegrationProperties
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

@Component
class JellyfinPluginAuthenticator(
    private val properties: JellyfinIntegrationProperties,
) {
    fun authenticate(token: String?) {
        if (!properties.enabled) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Jellyfin integration is disabled")
        }

        if (properties.pluginToken.isNotBlank() && token != properties.pluginToken) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid plugin token")
        }
    }
}
