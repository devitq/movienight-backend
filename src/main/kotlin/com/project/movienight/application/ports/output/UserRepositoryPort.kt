package com.project.movienight.application.ports.output

import com.project.movienight.domain.model.User
import java.util.UUID

interface UserRepositoryPort {
    fun save(user: User): User

    fun findById(id: UUID): User?

    fun findAll(): List<User>

    fun deleteById(id: UUID)
}
