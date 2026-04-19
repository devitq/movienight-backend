package com.project.movienight.adapters.web

import com.project.movienight.adapters.web.dto.request.CreateFilmRequest
import com.project.movienight.adapters.web.dto.request.EditFilmRequest
import com.project.movienight.adapters.web.dto.response.FilmResponse
import com.project.movienight.application.ports.input.CreateFilmCommand
import com.project.movienight.application.ports.input.CreateFilmUseCase
import com.project.movienight.application.ports.input.DeleteFilmUseCase
import com.project.movienight.application.ports.input.EditFilmCommand
import com.project.movienight.application.ports.input.EditFilmUseCase
import com.project.movienight.application.services.FilmService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/films")
class FilmController(
    private val createFilmUseCase: CreateFilmUseCase,
    private val editFilmUseCase: EditFilmUseCase,
    private val deleteFilmUseCase: DeleteFilmUseCase,
    private val filmService: FilmService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: CreateFilmRequest,
    ): FilmResponse =
        FilmResponse.fromDomain(
            createFilmUseCase.create(
                CreateFilmCommand(
                    title = request.title,
                    description = request.description,
                ),
            ),
        )

    @PatchMapping("/{id}")
    fun edit(
        @PathVariable id: UUID,
        @RequestBody request: EditFilmRequest,
    ): FilmResponse =
        FilmResponse.fromDomain(
            editFilmUseCase.edit(
                id = id,
                command =
                    EditFilmCommand(
                        title = request.title,
                        description = request.description,
                    ),
            ),
        )

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: UUID,
    ) = deleteFilmUseCase.delete(id)

    @GetMapping("/search")
    fun searchByTitle(
        @RequestParam title: String,
    ): FilmResponse? =
        filmService.findByTitle(title)?.let { FilmResponse.fromDomain(it) }
}
