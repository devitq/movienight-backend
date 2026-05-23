package com.project.movienight.adapters.persistence.entity

import com.project.movienight.domain.model.AuthProvider
import com.project.movienight.domain.model.User
import java.time.LocalDateTime
import java.util.UUID

data class UserEntity(
    val id: UUID,
    val name: String,
    val email: String,
    val provider: String?,
    val providerId: String?,
    val jellyfinUserId: String?,
    val createdAt: LocalDateTime,
)

fun UserEntity.toDomain(): User =
    User(
        id = id,
        name = name,
        email = email,
        preferences = null,
        jellyfinUserId = jellyfinUserId,
    )

fun User.toEntity(
    provider: AuthProvider? = null,
    providerId: String? = null,
    createdAt: LocalDateTime = LocalDateTime.now(),
): UserEntity =
    UserEntity(
        id = id,
        name = name,
        email = email,
        provider = provider?.name,
        providerId = providerId,
        jellyfinUserId = jellyfinUserId,
        createdAt = createdAt,
    )
