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
            provider = rs.getString("provider"),
            providerId = rs.getString("provider_id"),
            jellyfinUserId = rs.getString("jellyfin_user_id"),
            createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
        )
    }

    override fun save(user: User): User {
        val existingUser = findById(user.id)

        val entity =
            if (existingUser != null) {
                val existingEntity = existingUser.toEntity()
                user.toEntity(
                    provider = existingEntity.provider?.let { AuthProvider.valueOf(it) },
                    providerId = existingEntity.providerId,
                    createdAt = existingEntity.createdAt,
                )
            } else {
                user.toEntity()
            }

        val updatedRows =
            jdbc.update(
                """
                UPDATE users
                SET name = ?, email = ?, provider = ?, provider_id = ?, jellyfin_user_id = ?
                WHERE id = ?
                """.trimIndent(),
                entity.name,
                entity.email,
                entity.provider,
                entity.providerId,
                entity.jellyfinUserId,
                entity.id,
            )

        if (updatedRows == 0) {
            jdbc.update(
                """
                INSERT INTO users (id, name, email, provider, provider_id, jellyfin_user_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                entity.id,
                entity.name,
                entity.email,
                entity.provider,
                entity.providerId,
                entity.jellyfinUserId,
                entity.createdAt,
            )
        }
        return user
    }

    override fun findById(id: UUID): User? {
        val entities =
            jdbc.query(
                "SELECT id, name, email, provider, provider_id, jellyfin_user_id, created_at FROM users WHERE id = ?",
                userEntityRowMapper,
                id,
            )
        return entities.firstOrNull()?.toDomain()
    }

    override fun findByEmail(email: String): User? {
        val entities =
            jdbc.query(
                "SELECT id, name, email, provider, provider_id, created_at FROM users WHERE email = ?",
                userEntityRowMapper,
                email,
            )
        return entities.firstOrNull()?.toDomain()
    }

    override fun findAll(): List<User> =
        jdbc
            .query(
                "SELECT id, name, email, provider, provider_id, jellyfin_user_id, created_at FROM users",
                userEntityRowMapper,
            ).map { it.toDomain() }

    override fun deleteById(id: UUID) {
        jdbc.update("DELETE FROM users WHERE id = ?", id)
    }

    override fun findByProviderAndProviderId(
        provider: AuthProvider,
        providerId: String,
    ): User? {
        val entities =
            jdbc.query(
                """
                SELECT id, name, email, provider, provider_id, jellyfin_user_id, created_at FROM users
                WHERE provider = ? AND provider_id = ?
                """.trimIndent(),
                userEntityRowMapper,
                provider.name,
                providerId,
            )
        return entities.firstOrNull()?.toDomain()
    }
}
