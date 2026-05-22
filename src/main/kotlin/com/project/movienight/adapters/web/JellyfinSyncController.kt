package com.project.movienight.adapters.web

import com.project.movienight.adapters.web.dto.request.JellyfinSyncRequest
import com.project.movienight.application.ports.input.JellyfinSyncUseCase
import com.project.movienight.config.JellyfinIntegrationProperties
import com.project.movienight.domain.model.JellyfinSyncState
import com.project.movienight.domain.model.JellyfinSyncSummary
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/integrations/jellyfin")
class JellyfinSyncController(
    private val jellyfinSyncUseCase: JellyfinSyncUseCase,
    private val properties: JellyfinIntegrationProperties,
) {
    @PostMapping("/sync")
    fun pushSync(
        @RequestHeader(value = "X-MovieNight-Plugin-Token", required = false) token: String?,
        @Valid @RequestBody request: JellyfinSyncRequest,
    ): JellyfinSyncSummary {
        requireJellyfinIntegrationEnabled(properties)
        requireJellyfinPluginToken(properties, token)
        return jellyfinSyncUseCase.sync(request.toCommand())
    }

    @GetMapping("/sync-state")
    fun syncState(
        @RequestHeader(value = "X-MovieNight-Plugin-Token", required = false) token: String?,
    ): List<JellyfinSyncState> {
        requireJellyfinIntegrationEnabled(properties)
        requireJellyfinPluginToken(properties, token)
        return jellyfinSyncUseCase.getSyncStates()
    }
}
