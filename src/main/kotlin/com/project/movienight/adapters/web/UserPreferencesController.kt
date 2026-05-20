package com.project.movienight.adapters.web

import com.project.movienight.adapters.web.dto.request.UpsertUserPreferencesRequest
import com.project.movienight.adapters.web.dto.response.UserPreferencesResponse
import com.project.movienight.application.ports.input.GetUserPreferencesUseCase
import com.project.movienight.application.ports.input.UpsertUserPreferencesCommand
import com.project.movienight.application.ports.input.UpsertUserPreferencesUseCase
import com.project.movienight.domain.model.ContentType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/users/{userId}/preferences")
class UserPreferencesController(
    private val upsertUserPreferencesUseCase: UpsertUserPreferencesUseCase,
    private val getUserPreferencesUseCase: GetUserPreferencesUseCase,
) {
    @PutMapping
    fun upsert(
        @PathVariable userId: UUID,
        @RequestBody request: UpsertUserPreferencesRequest,
    ): UserPreferencesResponse =
        UserPreferencesResponse.fromDomain(
            upsertUserPreferencesUseCase.upsert(
                UpsertUserPreferencesCommand(
                    userId = userId,
                    weightedGenres = request.weightedGenres,
                    plotTypes = request.plotTypes,
                    eras = request.eras,
                    castAndDirectors = request.castAndDirectors,
                    moods = request.moods,
                    contentTypes =
                        request.contentTypes.mapNotNull {
                            runCatching {
                                ContentType.valueOf(
                                    it,
                                )
                            }.getOrNull()
                        },
                ),
            ),
        )

    @GetMapping
    fun get(
        @PathVariable userId: UUID,
    ): UserPreferencesResponse? = getUserPreferencesUseCase.get(userId)?.let { UserPreferencesResponse.fromDomain(it) }
}
