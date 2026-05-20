package com.project.movienight.application.ports.output

import com.project.movienight.domain.model.UserPreferences
import java.util.UUID

interface UserPreferencesRepositoryPort {
    fun save(preferences: UserPreferences): UserPreferences

    fun findByUserId(userId: UUID): UserPreferences?
}
