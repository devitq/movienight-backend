package com.project.movienight.adapters.persistence.entity

import com.project.movienight.domain.model.AuthProvider
import com.project.movienight.domain.model.User
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserEntityMappingTest {
    @Test
    fun `toDomain maps UserEntity correctly`() {
        val entity =
            UserEntity(
                id = UUID.randomUUID(),
                name = "John Pork",
                email = "john@email.com",
                provider = "GOOGLE",
                providerId = "google1234",
                createdAt = LocalDateTime.now(),
            )
        val user = entity.toDomain()

        assertEquals(entity.id, user.id)
        assertEquals(entity.name, user.name)
        assertEquals(entity.email, user.email)
        assertNull(user.library)
    }

    @Test
    fun `toEntity maps User with OAuth provider`() {
        val user =
            User(
                id = UUID.randomUUID(),
                name = "Jane",
                email = "jane@mail.com",
                library = null,
            )

        val entity = user.toEntity(AuthProvider.YANDEX, "yandex456")

        assertEquals(user.id, entity.id)
        assertEquals(user.name, entity.name)
        assertEquals(user.email, entity.email)
        assertEquals("YANDEX", entity.provider)
        assertEquals("yandex456", entity.providerId)
    }

    @Test
    fun `toEntity maps User without OAuth provider`() {
        val user =
            User(
                id = UUID.randomUUID(),
                name = "Bob",
                email = "bob@mail.com",
                library = null,
            )

        val entity = user.toEntity()

        assertNull(entity.provider)
        assertNull(entity.providerId)
    }

    @Test
    fun `mapping is reversible for basic fields`() {
        val original =
            User(
                id = UUID.randomUUID(),
                name = "Alice",
                email = "alice@email.com",
                library = null,
            )

        val mapped = original.toEntity().toDomain()

        assertEquals(original.id, mapped.id)
        assertEquals(original.name, mapped.name)
        assertEquals(original.email, mapped.email)
    }
}
