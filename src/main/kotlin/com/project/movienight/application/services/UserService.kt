package com.project.movienight.application.services

import com.project.movienight.application.ports.input.CreateUserCommand
import com.project.movienight.application.ports.input.CreateUserUseCase
import com.project.movienight.application.ports.input.DeleteUserUseCase
import com.project.movienight.application.ports.input.EditUserCommand
import com.project.movienight.application.ports.input.EditUserUseCase
import com.project.movienight.application.ports.input.GetAllUsersUseCase
import com.project.movienight.application.ports.input.GetUserByIdUseCase
import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.application.ports.output.UserRepositoryPort
import com.project.movienight.config.UserServiceProperties
import com.project.movienight.domain.exception.BlockedValueException
import com.project.movienight.domain.exception.EntityNotFoundException
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
    DeleteUserUseCase,
    GetUserByIdUseCase,
    GetAllUsersUseCase {
    override fun create(command: CreateUserCommand): User {
        if (userConfig.isBlocked(command.name)) {
            throw BlockedValueException(target = "User", field = "name")
        }

        val user =
            User(
                id = idGenerator.generateId(),
                name = command.name,
                email = command.email,
                password = "",
                library = null,
            )
        return userRepository.save(user)
    }

    override fun edit(
        id: UUID,
        command: EditUserCommand,
    ): User {
        if (userConfig.isBlocked(command.name)) {
            throw BlockedValueException(target = "User", field = "name")
        }

        var user = userRepository.findById(id) ?: throw EntityNotFoundException(entity = "User", id = id.toString())
        user = user.copy(name = command.name)
        return userRepository.save(user)
    }

    override fun delete(id: UUID) {
        userRepository.findById(id) ?: throw EntityNotFoundException(entity = "User", id = id.toString())
        userRepository.deleteById(id)
    }

    override fun getById(id: UUID): User =
        userRepository.findById(id) ?: throw EntityNotFoundException(entity = "User", id = id.toString())

    override fun getAll(): List<User> = userRepository.findAll()
}
