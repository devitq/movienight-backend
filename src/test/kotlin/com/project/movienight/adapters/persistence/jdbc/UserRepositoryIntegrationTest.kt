package com.project.movienight.adapters.persistence.jdbc

import com.project.movienight.domain.model.AuthProvider
import com.project.movienight.domain.model.User
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
class UserRepositoryIntegrationTest {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setup() {
        cleanDatabase()
    }

    @AfterEach
    fun cleanup() {
        cleanDatabase()
    }

    private fun cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM favorites")
        jdbcTemplate.execute("DELETE FROM films")
        jdbcTemplate.execute("DELETE FROM users")
    }

    @Test
    fun `should save new user and return saved user`() {
        val user =
            User(
                id = UUID.randomUUID(),
                name = "John Doe",
                email = "john@example.com",
            )

        val savedUser = userRepository.save(user)

        assertNotNull(savedUser)
        assertEquals(user.id, savedUser.id)
        assertEquals(user.name, savedUser.name)
        assertEquals(user.email, savedUser.email)
    }

    @Test
    fun `should update existing user`() {
        // given
        val userId = UUID.randomUUID()
        val originalUser = User(userId, "John Doe", "john@example.com")
        userRepository.save(originalUser)

        val updatedUser = User(userId, "Jane Doe", "jane@example.com")
        val result = userRepository.save(updatedUser)

        assertEquals(userId, result.id)
        assertEquals("Jane Doe", result.name)
        assertEquals("jane@example.com", result.email)

        val foundUser = userRepository.findById(userId)
        assertNotNull(foundUser)
        assertEquals("Jane Doe", foundUser?.name)
        assertEquals("jane@example.com", foundUser?.email)
    }

    @Test
    fun `should find user by id`() {
        // given
        val user = User(UUID.randomUUID(), "John Doe", "john@example.com")
        userRepository.save(user)

        // when
        val foundUser = userRepository.findById(user.id)

        // then
        assertNotNull(foundUser)
        assertEquals(user.id, foundUser?.id)
        assertEquals(user.name, foundUser?.name)
        assertEquals(user.email, foundUser?.email)
    }

    @Test
    fun `should return null when user not found by id`() {
        // given
        val nonExistentId = UUID.randomUUID()

        // when
        val foundUser = userRepository.findById(nonExistentId)

        // then
        assertNull(foundUser)
    }

    @Test
    fun `should find all users`() {
        // given
        val user1 = User(UUID.randomUUID(), "John Doe", "john@example.com")
        val user2 = User(UUID.randomUUID(), "Jane Smith", "jane@example.com")
        val user3 = User(UUID.randomUUID(), "Bob Johnson", "bob@example.com")

        userRepository.save(user1)
        userRepository.save(user2)
        userRepository.save(user3)

        // when
        val allUsers = userRepository.findAll()

        // then
        assertEquals(3, allUsers.size)
        assertTrue(allUsers.any { it.id == user1.id })
        assertTrue(allUsers.any { it.id == user2.id })
        assertTrue(allUsers.any { it.id == user3.id })
    }

    @Test
    fun `should return empty list when no users exist`() {
        // when
        val allUsers = userRepository.findAll()

        // then
        assertTrue(allUsers.isEmpty())
    }

    @Test
    fun `should delete user by id`() {
        // given
        val user = User(UUID.randomUUID(), "John Doe", "john@example.com")
        userRepository.save(user)

        // when
        userRepository.deleteById(user.id)

        // then
        val foundUser = userRepository.findById(user.id)
        assertNull(foundUser)
    }

    @Test
    fun `should not throw exception when deleting non-existent user`() {
        // given
        val nonExistentId = UUID.randomUUID()

        // when & then (no exception should be thrown)
        userRepository.deleteById(nonExistentId)
    }

    @Test
    fun `should create OAuth user with provider identity`() {
        val user = User(UUID.randomUUID(), "OAuth User", "oauth@example.com")

        val savedUser = userRepository.createOAuthUser(user, AuthProvider.GOOGLE, "google-123")

        assertEquals(user.id, savedUser.id)
        assertEquals(user.email, savedUser.email)

        val foundByProvider = userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-123")
        assertNotNull(foundByProvider)
        assertEquals(user.id, foundByProvider?.id)
    }

    @Test
    fun `should link OAuth account to existing user`() {
        val user = userRepository.save(User(UUID.randomUUID(), "Link User", "link@example.com"))

        val linkedUser = userRepository.linkOAuthAccount(user.id, AuthProvider.YANDEX, "yandex-456")

        assertEquals(user.id, linkedUser.id)
        val foundByProvider = userRepository.findByProviderAndProviderId(AuthProvider.YANDEX, "yandex-456")
        assertNotNull(foundByProvider)
        assertEquals(user.id, foundByProvider?.id)
    }

    @Test
    fun `find by email should include jellyfin user id`() {
        val user = userRepository.save(User(UUID.randomUUID(), "Jellyfin User", "jellyfin@example.com"))
        jdbcTemplate.update("UPDATE users SET jellyfin_user_id = ? WHERE id = ?", "jellyfin-789", user.id)

        val foundUser = userRepository.findByEmail(user.email)

        assertNotNull(foundUser)
        assertEquals("jellyfin-789", foundUser?.jellyfinUserId)
    }
}
