package com.project.movienight.application.ports.input

import com.project.movienight.domain.model.Film
import java.util.UUID

interface CreateFilmUseCase {
    fun create(command: CreateFilmCommand): Film
}

data class CreateFilmCommand(
    val title: String,
    val description: String,
)

interface EditFilmUseCase {
    fun edit(
        id: UUID,
        command: EditFilmCommand,
    ): Film
}

data class EditFilmCommand(
    val title: String,
    val description: String,
)

interface DeleteFilmUseCase {
    fun delete(id: UUID)
}
