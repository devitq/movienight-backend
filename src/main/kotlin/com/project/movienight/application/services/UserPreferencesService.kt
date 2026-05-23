package com.project.movienight.application.services

import com.project.movienight.application.ports.input.UpsertUserPreferencesCommand
import com.project.movienight.application.ports.input.UserPreferencesUseCase
import com.project.movienight.application.ports.output.UserPreferencesRepositoryPort
import com.project.movienight.domain.model.UserPreferences
import org.springframework.stereotype.Service

@Service
class UserPreferencesService(
    private val userPreferencesRepository: UserPreferencesRepositoryPort,
) : UserPreferencesUseCase {
    override fun upsert(command: UpsertUserPreferencesCommand): UserPreferences =
        userPreferencesRepository.save(
            UserPreferences(
                userId = command.userId,
                weightedGenres = command.weightedGenres,
                plotTypes = command.plotTypes,
                eras = command.eras,
                castAndDirectors = command.castAndDirectors,
                moods = command.moods,
                contentTypes = command.contentTypes,
            ),
        )

    override fun get(userId: java.util.UUID): UserPreferences? = userPreferencesRepository.findByUserId(userId)
}
