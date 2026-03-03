package com.project.movienight.application.services

import com.project.movienight.application.ports.input.CreateUserCommand
import com.project.movienight.application.ports.input.CreateUserUseCase
import com.project.movienight.application.ports.input.DeleteUserUseCase
import com.project.movienight.application.ports.input.EditUserCommand
import com.project.movienight.application.ports.input.EditUserUseCase
import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.application.ports.output.UserRepositoryPort
import com.project.movienight.config.UserServiceProperties
import com.project.movienight.domain.model.User
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepositoryPort,
    private val idGenerator: IdGenerator,
    private val userConfig: UserServiceProperties,
) : CreateUserUseCase,
    EditUserUseCase,
    DeleteUserUseCase {
    override fun create(command: CreateUserCommand): User {
        if (userConfig.isBlocked(command.name)) {
            throw IllegalArgumentException("User with this name is not acceptable")
        }

        val user =
            User(
                id = idGenerator.generateId(),
                name = command.name,
                email = command.email,
                library = null,
            )
        return userRepository.save(user)
    }

    override fun edit(
        id: UUID,
        command: EditUserCommand,
    ): User {
        if (userConfig.isBlocked(command.name)) {
            throw IllegalArgumentException("User with this name is not acceptable")
        }

        var user = userRepository.findById(id) ?: throw IllegalArgumentException("User with id $id not found")

        user = user.copy(name = command.name)

        return userRepository.save(user)
    }

    override fun delete(id: UUID) {
        userRepository.findById(id) ?: throw IllegalArgumentException("User with id $id not found")

        userRepository.deleteById(id)
    }
}
