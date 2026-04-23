package com.project.movienight.adapters.persistence.jdbc

import com.project.movienight.adapters.persistence.entity.UserEntity
import com.project.movienight.adapters.persistence.entity.toDomain
import com.project.movienight.adapters.persistence.entity.toEntity
import com.project.movienight.application.ports.output.UserRepositoryPort
import com.project.movienight.domain.model.AuthProvider
import com.project.movienight.domain.model.User
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

@Repository
class UserRepository(
    private val jdbc: JdbcTemplate,
) : UserRepositoryPort {
    private val userEntityRowMapper = { rs: ResultSet, _: Int ->
        UserEntity(
            id = UUID.fromString(rs.getString("id")),
            name = rs.getString("name"),
            email = rs.getString("email"),
            password = rs.getString("password"),
            provider = rs.getString("provider"),
            providerId = rs.getString("provider_id"),
            createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
        )
    }

    override fun save(user: User): User {
        val entity = user.toEntity()
        val updatedRows =
            jdbc.update(
                """
                UPDATE users
                SET name = ?, email = ?, password = ?, provider = ?, provider_id = ?
                WHERE id = ?
                """.trimIndent(),
                entity.name,
                entity.email,
                user.password,
                entity.provider,
                entity.providerId,
                entity.id,
            )
        if (updatedRows == 0) {
            jdbc.update(
                """
                INSERT INTO users (id, name, email, password, provider, provider_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                entity.id,
                entity.name,
                entity.email,
                user.password,
                entity.provider,
                entity.providerId,
                entity.createdAt,
            )
        }
        return user
    }

    override fun findById(id: UUID): User? {
        val entities =
            jdbc.query(
                "SELECT id, name, email, password, provider, provider_id, created_at FROM users WHERE id = ?",
                userEntityRowMapper,
                id,
            )
        return entities.firstOrNull()?.toDomain()
    }

    override fun findAll(): List<User> =
        jdbc
            .query(
                "SELECT id, name, email, password, provider, provider_id, created_at FROM users",
                userEntityRowMapper,
            ).map { it.toDomain() }

    override fun deleteById(id: UUID) {
        jdbc.update("DELETE FROM users WHERE id = ?", id)
    }

    override fun saveWithOAuth2(user: User, provider: String, providerId: String): User {
        val updatedRows = jdbc.update(
            """
            UPDATE users
            SET name = ?, email = ?, password = ?, provider = ?, provider_id = ?
            WHERE id = ?
            """.trimIndent(),
            user.name,
            user.email,
            user.password,
            provider,
            providerId,
            user.id,
        )
        if (updatedRows == 0) {
            jdbc.update(
                """
                INSERT INTO users (id, name, email, password, provider, provider_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                user.id,
                user.name,
                user.email,
                user.password,
                provider,
                providerId,
            )
        }
        return user
    }

    override fun findByProviderAndProviderId(provider: String, providerId: String): User? {
        val entities = jdbc.query(
            "SELECT id, name, email, password, provider, provider_id, created_at FROM users WHERE provider = ? AND provider_id = ?",
            userEntityRowMapper,
            provider,
            providerId,
        )
        return entities.firstOrNull()?.toDomain()
    }

    override fun findByProviderAndProviderId(
        provider: AuthProvider,
        providerId: String,
    ): User? {
        val entities =
            jdbc.query(
                """
                SELECT id, name, email, password, provider, provider_id, created_at
                FROM users
                WHERE provider = ? AND provider_id = ?
                """.trimIndent(),
                userEntityRowMapper,
                provider.name,
                providerId,
            )
        return entities.firstOrNull()?.toDomain()
    }
}
