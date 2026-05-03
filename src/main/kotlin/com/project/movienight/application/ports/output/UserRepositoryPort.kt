package com.project.movienight.application.ports.output

import com.project.movienight.domain.model.AuthProvider
import com.project.movienight.domain.model.User
import java.util.UUID

interface UserRepositoryPort {
    fun save(user: User): User

    fun saveWithOAuth2(user: User, provider: String, providerId: String): User

    fun findByProviderAndProviderId(provider: String, providerId: String): User?

    fun findById(id: UUID): User?

    fun findByEmail(email: String): User?

    fun findAll(): List<User>

    fun deleteById(id: UUID)

    fun findByProviderAndProviderId(
        provider: AuthProvider,
        providerId: String,
    ): User?
}
