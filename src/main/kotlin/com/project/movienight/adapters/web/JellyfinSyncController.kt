package com.project.movienight.adapters.web

import com.project.movienight.adapters.web.dto.request.JellyfinSyncRequest
import com.project.movienight.application.ports.input.IngestJellyfinSyncCommand
import com.project.movienight.application.ports.input.JellyfinSyncItemCommand
import com.project.movienight.application.ports.input.JellyfinSyncUseCase
import com.project.movienight.application.ports.input.JellyfinSyncUserCommand
import com.project.movienight.application.ports.input.JellyfinSyncUserStateCommand
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
    private val authenticator: JellyfinPluginAuthenticator,
) {
    @PostMapping("/sync")
    fun syncNow(
        @RequestHeader(value = "X-MovieNight-Plugin-Token", required = false) token: String?,
        @Valid @RequestBody(required = false) request: JellyfinSyncRequest?,
    ): JellyfinSyncSummary {
        authenticator.authenticate(token)
        return if (request == null) {
            jellyfinSyncUseCase.syncNow()
        } else {
            jellyfinSyncUseCase.ingest(request.toCommand())
        }
    }

    @GetMapping("/sync-state")
    fun syncState(
        @RequestHeader(value = "X-MovieNight-Plugin-Token", required = false) token: String?,
    ): List<JellyfinSyncState> {
        authenticator.authenticate(token)
        return jellyfinSyncUseCase.getSyncStates()
    }

    private fun JellyfinSyncRequest.toCommand(): IngestJellyfinSyncCommand =
        IngestJellyfinSyncCommand(
            users =
                users.map { user ->
                    JellyfinSyncUserCommand(
                        jellyfinUserId = user.jellyfinUserId,
                        name = user.name,
                    )
                },
            items =
                items.map { item ->
                    JellyfinSyncItemCommand(
                        jellyfinItemId = item.jellyfinItemId,
                        title = item.title,
                        originalTitle = item.originalTitle,
                        description = item.description,
                        year = item.year,
                        genres = item.genres,
                        imdbId = item.imdbId,
                        tmdbId = item.tmdbId,
                        jellyfinLibraryId = item.jellyfinLibraryId,
                        userStates =
                            item.userStates.map { state ->
                                JellyfinSyncUserStateCommand(
                                    jellyfinUserId = state.jellyfinUserId,
                                    isViewed = state.isViewed,
                                    playCount = state.playCount,
                                    lastPlayedAt = state.lastPlayedAt,
                                    userRating = state.userRating,
                                )
                            },
                    )
                },
        )
}
