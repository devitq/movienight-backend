package com.project.movienight.application.ports.output

import java.util.UUID

interface IdGenerator {
    fun generateId(): UUID
}
