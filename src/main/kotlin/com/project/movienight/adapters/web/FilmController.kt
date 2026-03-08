package com.project.movienight.adapters.web

import com.project.movienight.application.ports.input.CreateFilmCommand
import com.project.movienight.application.ports.input.CreateFilmUseCase
import com.project.movienight.application.ports.input.DeleteFilmUseCase
import com.project.movienight.application.ports.input.EditFilmCommand
import com.project.movienight.application.ports.input.EditFilmUseCase
import com.project.movienight.domain.model.Film
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/films")
class FilmController(
    private val createFilmUseCase: CreateFilmUseCase,
    private val editFilmUseCase: EditFilmUseCase,
    private val deleteFilmUseCase: DeleteFilmUseCase,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateFilmRequest): Film =
        createFilmUseCase.create(
            CreateFilmCommand(
                title = request.title,
                description = request.description,
            )
        )

    @PatchMapping("/{id}")
    fun edit(
        @PathVariable id: UUID,
        @RequestBody request: EditFilmRequest,
    ): Film =
        editFilmUseCase.edit(
            id = id,
            command = EditFilmCommand(
                title = request.title,
                description = request.description,
            )
        )

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) =
        deleteFilmUseCase.delete(id)
}

data class CreateFilmRequest(
    val title: String,
    val description: String,
)

data class EditFilmRequest(
    val title: String,
    val description: String,
)
