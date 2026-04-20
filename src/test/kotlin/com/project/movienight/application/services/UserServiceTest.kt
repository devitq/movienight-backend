package com.project.movienight.application.services

import com.project.movienight.application.ports.input.CreateUserCommand
import com.project.movienight.application.ports.input.EditUserCommand
import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.application.ports.output.UserRepositoryPort
import com.project.movienight.config.UserServiceProperties
import com.project.movienight.domain.exception.BlockedValueException
import com.project.movienight.domain.exception.EntityNotFoundException
import com.project.movienight.domain.model.User
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class UserServiceTest {
    private lateinit var userRepository: UserRepositoryPort
    private lateinit var idGenerator: IdGenerator
    private lateinit var userConfig: UserServiceProperties
    private lateinit var userService: UserService

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        idGenerator = mockk()
        userConfig = mockk()
        userService = UserService(userRepository, idGenerator, userConfig)
    }

    @Test
    fun `should create user successfully`() {
        val command = CreateUserCommand(name = "John Doe", email = "john@example.com")
        val userId = UUID.randomUUID()
        val expectedUser = User(id = userId, name = "John Doe", email = "john@example.com", library = null)

        every { userConfig.isBlocked("John Doe") } returns false
        every { idGenerator.generateId() } returns userId
        every { userRepository.save(any()) } returns expectedUser

        val result = userService.create(command)

        assertNotNull(result)
        assertEquals(userId, result.id)
        assertEquals("John Doe", result.name)
        assertEquals("john@example.com", result.email)

        verify(exactly = 1) { userConfig.isBlocked("John Doe") }
        verify(exactly = 1) { idGenerator.generateId() }
        verify(exactly = 1) { userRepository.save(any()) }
    }

    @Test
    fun `should throw BlockedValueException when creating user with blocked name`() {
        val command = CreateUserCommand(name = "admin", email = "admin@example.com")

        every { userConfig.isBlocked("admin") } returns true

        assertThrows<BlockedValueException> {
            userService.create(command)
        }

        verify(exactly = 1) { userConfig.isBlocked("admin") }
        verify(exactly = 0) { idGenerator.generateId() }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `should edit user successfully`() {
        val userId = UUID.randomUUID()
        val command = EditUserCommand(name = "Jane Doe")
        val existingUser = User(id = userId, name = "John Doe", email = "john@example.com", library = null)
        val updatedUser = User(id = userId, name = "Jane Doe", email = "john@example.com", library = null)

        every { userConfig.isBlocked("Jane Doe") } returns false
        every { userRepository.findById(userId) } returns existingUser
        every { userRepository.save(any()) } returns updatedUser

        val result = userService.edit(userId, command)

        assertNotNull(result)
        assertEquals(userId, result.id)
        assertEquals("Jane Doe", result.name)

        verify(exactly = 1) { userConfig.isBlocked("Jane Doe") }
        verify(exactly = 1) { userRepository.findById(userId) }
        verify(exactly = 1) { userRepository.save(any()) }
    }

    @Test
    fun `should throw BlockedValueException when editing user with blocked name`() {
        val userId = UUID.randomUUID()
        val command = EditUserCommand(name = "root")

        every { userConfig.isBlocked("root") } returns true

        assertThrows<BlockedValueException> {
            userService.edit(userId, command)
        }

        verify(exactly = 1) { userConfig.isBlocked("root") }
        verify(exactly = 0) { userRepository.findById(any()) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `should throw EntityNotFoundException when editing non-existent user`() {
        val userId = UUID.randomUUID()
        val command = EditUserCommand(name = "Jane Doe")

        every { userConfig.isBlocked("Jane Doe") } returns false
        every { userRepository.findById(userId) } returns null

        assertThrows<EntityNotFoundException> {
            userService.edit(userId, command)
        }

        verify(exactly = 1) { userConfig.isBlocked("Jane Doe") }
        verify(exactly = 1) { userRepository.findById(userId) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `should delete user successfully`() {
        val userId = UUID.randomUUID()
        val existingUser = User(id = userId, name = "John Doe", email = "john@example.com", library = null)

        every { userRepository.findById(userId) } returns existingUser
        justRun { userRepository.deleteById(userId) }

        userService.delete(userId)

        verify(exactly = 1) { userRepository.findById(userId) }
        verify(exactly = 1) { userRepository.deleteById(userId) }
    }

    @Test
    fun `should throw EntityNotFoundException when deleting non-existent user`() {
        val userId = UUID.randomUUID()

        every { userRepository.findById(userId) } returns null

        assertThrows<EntityNotFoundException> {
            userService.delete(userId)
        }

        verify(exactly = 1) { userRepository.findById(userId) }
        verify(exactly = 0) { userRepository.deleteById(any()) }
    }
}
