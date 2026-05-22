package com.project.movienight.application.ports.output

import com.project.movienight.domain.model.JellyfinSyncState
import java.util.UUID

interface JellyfinSyncStateRepositoryPort {
    fun save(state: JellyfinSyncState): JellyfinSyncState

    fun findByUserId(userId: UUID): JellyfinSyncState?

    fun findAll(): List<JellyfinSyncState>
}
