package com.project.movienight.adapters.web

import com.project.movienight.application.services.JellyfinSyncService
import com.project.movienight.domain.model.JellyfinSyncState
import com.project.movienight.domain.model.JellyfinSyncSummary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/integrations/jellyfin")
class JellyfinSyncController(
    private val jellyfinSyncService: JellyfinSyncService,
) {
    @PostMapping("/sync")
    fun syncNow(): JellyfinSyncSummary = jellyfinSyncService.syncNow()

    @GetMapping("/sync-state")
    fun syncState(): List<JellyfinSyncState> = jellyfinSyncService.getSyncStates()
}
