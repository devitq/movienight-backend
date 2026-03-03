package com.project.movienight.adapters.id

import com.project.movienight.application.ports.output.IdGenerator
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UuidGenerator : IdGenerator {
    override fun generateId(): UUID = UUID.randomUUID()
}
