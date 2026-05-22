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
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(javaClass)

    override fun create(command: CreateUserCommand): User {
        log.info("Creating new user with email: {}", command.email)
        log.debug("Create user request: name='{}', email='{}'", command.name, command.email)

        if (userConfig.isBlocked(command.name)) {
            log.warn("User creation blocked: name contains blocked pattern '{}'", command.name)
            throw BlockedValueException(target = "User", field = "name")
        }

        val user =
            User(
                id = idGenerator.generateId(),
                name = command.name,
                email = command.email,
                library = null,
                jellyfinUserId = null,
            )
        val saved = userRepository.save(user)

        log.info("User created successfully: id={}, email='{}'", saved.id, saved.email)
        return saved
    }

    override fun edit(
        id: UUID,
        command: EditUserCommand,
    ): User {
        log.info("Editing user: id={}", id)
        log.debug("Edit user request: id={}, name='{}', jellyfinUserId={}", id, command.name, command.jellyfinUserId)

        if (userConfig.isBlocked(command.name)) {
            log.warn("User edit blocked: name contains blocked pattern '{}'", command.name)
            throw BlockedValueException(target = "User", field = "name")
        }

        var user =
            userRepository.findById(id)
                ?: throw EntityNotFoundException(entity = "User", id = id.toString())

        log.debug("Existing user found: id={}, current name='{}'", user.id, user.name)

        user =
            user.copy(
                name = command.name,
                jellyfinUserId = command.jellyfinUserId ?: user.jellyfinUserId,
            )

        val saved = userRepository.save(user)
        log.info("User edited successfully: id={}, new name='{}'", saved.id, saved.name)
        return saved
    }

    override fun delete(id: UUID) {
        log.info("Deleting user: id={}", id)
        log.debug("Delete user request: id={}", id)

        val user =
            userRepository.findById(id)
                ?: throw EntityNotFoundException(entity = "User", id = id.toString())

        log.debug("User found for deletion: id={}, email='{}'", user.id, user.email)

        userRepository.deleteById(id)
        log.info("User deleted successfully: id={}", id)
    }

    override fun getById(id: UUID): User {
        log.debug("Fetching user by id: {}", id)
        val user =
            userRepository.findById(id)
                ?: throw EntityNotFoundException(entity = "User", id = id.toString())
        log.debug("User found: id={}, name='{}', email='{}'", user.id, user.name, user.email)
        return user
    }

    override fun getAll(): List<User> {
        log.debug("Fetching all users")
        val users = userRepository.findAll()
        log.info("Retrieved {} users from database", users.size)
        return users
    }
}
