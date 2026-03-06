package com.project.movienight.adapters.persistence

import com.project.movienight.domain.model.Film
import com.project.movienight.domain.model.User
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class UserRepository(private val jdbc: JdbcTemplate) {

    fun findById(id: UUID): User? =
        jdbc.queryForObject(
            "SELECT * FROM users WHERE id = ?",
            {rs, _ ->
                User(
                    id = UUID.fromString(rs.getString("id")),
                    name = rs.getString("name"),
                    email = rs.getString("email"),
                    library = null // брать библиотеку пользователя лучше отдельным запросом,
                                   // который будет в FilmLibraryRepository
                )
            },
            id.toString()
        )

    fun save(user: User) {
        jdbc.update(
            """
            INSERT INTO users(id, name, email, library)
            VALUES (:id, :name, :email, :library)
            ON CONFLICT (id)
            DO UPDATE SET name = :name, email = :email, library = :library
        """,
            user.id.toString(), user.name, user.email, user.library,
            user.name, user.email, user.library
        )
    }

    fun deleteById(id: UUID) {
        jdbc.update(
            "DELETE FROM users WHERE id = ?",
            id.toString()
        )
    }

    fun findByEmail(email: String): User? =
        jdbc.queryForObject(
            "SELECT * FROM users WHERE email = ?",
            {rs, _ ->
                User(
                    id = UUID.fromString(rs.getString("id")),
                    name = rs.getString("name"),
                    email = rs.getString("email"),
                    library = null
                )
            },

        )
    fun findTopN(limit: Int, sortBy: String = "name"): List<User> =
        jdbc.query(
            "SELECT * FROM users ORDER BY $sortBy LIMIT ?",
            { rs, _ ->
                User(
                    id = UUID.fromString(rs.getString("id")),
                    name = rs.getString("name"),
                    email = rs.getString("email"),
                    library = null
                )
            },
            limit
        )

    fun existsByEmail(email: String): Boolean {
        val count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM users WHERE email = ?",
            Int::class.java,
            email
        ) ?: 0
        return count > 0
    }

}
