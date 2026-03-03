package com.project.movienight.application.ports.input

import com.project.movienight.domain.model.User
import java.util.UUID

interface CreateUserUseCase {
    fun create(command: CreateUserCommand): User
}

data class CreateUserCommand(
    val name: String,
    val email: String,
)

interface EditUserUseCase {
    fun edit(
        id: UUID,
        command: EditUserCommand,
    ): User
}

data class EditUserCommand(
    val name: String,
)

interface DeleteUserUseCase {
    fun delete(id: UUID)
}
