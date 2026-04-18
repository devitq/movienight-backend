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
            library = null,
        )
    }

    override fun save(user: User): User {
        val updatedRows =
            jdbc.update(
                """
                UPDATE users
                SET name = ?, email = ?
                WHERE id = ?
                """.trimIndent(),
                user.name,
                user.email,
                user.id,
            )
        if (updatedRows == 0) {
            jdbc.update(
                """
                INSERT INTO users (id, name, email)
                VALUES (?, ?, ?)
                """.trimIndent(),
                user.id,
                user.name,
                user.email,
            )
        }
        return user
    }

    override fun findById(id: UUID): User? {
        val users =
            jdbc.query(
                "SELECT id, name, email FROM users WHERE id = ?",
                userRowMapper,
                id,
            )
        return users.firstOrNull()
    }

    override fun findAll(): List<User> =
        jdbc.query(
            "SELECT id, name, email FROM users",
            userRowMapper,
        )

    override fun deleteById(id: UUID) {
        jdbc.update("DELETE FROM users WHERE id = ?", id)
    }
}
