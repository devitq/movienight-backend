package com.project.movienight.adapters.persistence.jdbc

import com.project.movienight.application.ports.output.UserRepositoryPort
import com.project.movienight.domain.model.User
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

@Repository
class UserRepository(
    private val jdbc: JdbcTemplate,
) : UserRepositoryPort {
    private val userRowMapper = { rs: ResultSet, _: Int ->
        User(
            id = UUID.fromString(rs.getString("id")),
            name = rs.getString("name"),
            email = rs.getString("email"),
            password = rs.getString("password"),
            library = null,
        )
    }

    override fun save(user: User): User {
        val updatedRows =
            jdbc.update(
                """
                UPDATE users
                SET name = ?, email = ?, password = ?
                WHERE id = ?
                """.trimIndent(),
                user.name,
                user.email,
                user.password,
                user.id,
            )
        if (updatedRows == 0) {
            jdbc.update(
                """
                INSERT INTO users (id, name, email, password)
                VALUES (?, ?, ?, ?)
                """.trimIndent(),
                user.id,
                user.name,
                user.email,
                user.password,
            )
        }
        return user
    }

    override fun findById(id: UUID): User? {
        val users =
            jdbc.query(
                "SELECT id, name, email, password FROM users WHERE id = ?",
                userRowMapper,
                id,
            )
        return users.firstOrNull()
    }

    override fun findByEmail(email: String): User? {
        val users = jdbc.query(
            "SELECT id, name, email, password FROM users WHERE email = ?",
            userRowMapper,
            email,
        )
        return users.firstOrNull()
    }

    override fun findAll(): List<User> =
        jdbc.query(
            "SELECT id, name, email, password FROM users",
            userRowMapper,
        )

    override fun deleteById(id: UUID) {
        jdbc.update("DELETE FROM users WHERE id = ?", id)
    }

    override fun saveWithOAuth2(user: User, provider: String, providerId: String): User {
        val updatedRows = jdbc.update("""
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
        val users = jdbc.query(
            "SELECT id, name, email, password FROM users WHERE provider = ? AND provider_id = ?",
            userRowMapper,
            provider,
            providerId,
        )
        return users.firstOrNull()
    }
}
