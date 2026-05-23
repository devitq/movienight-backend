package com.project.movienight.application.ports.input

import com.project.movienight.domain.model.User
import java.util.UUID

interface UserUseCase {
    fun create(command: CreateUserCommand): User

    fun edit(
        id: UUID,
        command: EditUserCommand,
    ): User

    fun delete(id: UUID)

    fun getById(id: UUID): User

    fun getAll(): List<User>
}

data class CreateUserCommand(
    val name: String,
    val email: String,
)

data class EditUserCommand(
    val name: String,
    val jellyfinUserId: String? = null,
)
