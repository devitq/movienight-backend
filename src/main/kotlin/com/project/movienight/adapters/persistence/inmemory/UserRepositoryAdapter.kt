package com.project.movienight.adapters.persistence.inmemory

import com.project.movienight.application.ports.output.UserRepositoryPort
import com.project.movienight.domain.model.User
import org.springframework.stereotype.Repository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Repository
class UserRepositoryAdapter : UserRepositoryPort {
    private val users: MutableMap<UUID, User> = ConcurrentHashMap()

    override fun save(user: User): User {
        users[user.id] = user

        return user
    }

    override fun findById(id: UUID): User? = users[id]

    override fun findAll(): List<User> = users.values.toList()

    override fun deleteById(id: UUID) {
        users.remove(id)
    }
}
