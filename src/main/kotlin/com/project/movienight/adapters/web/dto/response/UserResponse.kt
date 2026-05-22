package com.project.movienight.adapters.web.dto.response

import com.project.movienight.domain.model.User
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val name: String,
    val email: String,
    val jellyfinUserId: String?,
) {
    companion object {
        fun fromDomain(user: User): UserResponse =
            UserResponse(
                id = user.id,
                name = user.name,
                email = user.email,
                jellyfinUserId = user.jellyfinUserId,
            )
    }
}
