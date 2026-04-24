package com.project.movienight.adapters.web

import com.project.movienight.adapters.web.dto.request.CreateFilmRequest
import com.project.movienight.adapters.web.dto.request.EditFilmRequest
import com.project.movienight.adapters.web.dto.response.FilmResponse
import com.project.movienight.application.ports.input.CreateFilmCommand
import com.project.movienight.application.ports.input.CreateFilmUseCase
import com.project.movienight.application.ports.input.DeleteFilmUseCase
import com.project.movienight.application.ports.input.EditFilmCommand
import com.project.movienight.application.ports.input.EditFilmUseCase
import com.project.movienight.application.ports.input.GetAllFilmsUseCase
import com.project.movienight.application.ports.input.GetFilmByIdUseCase
import com.project.movienight.application.ports.input.SearchFilmByTitleUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/films")
class FilmController(
    private val createFilmUseCase: CreateFilmUseCase,
    private val editFilmUseCase: EditFilmUseCase,
    private val deleteFilmUseCase: DeleteFilmUseCase,
    private val getFilmByIdUseCase: GetFilmByIdUseCase,
    private val getAllFilmsUseCase: GetAllFilmsUseCase,
    private val searchFilmByTitleUseCase: SearchFilmByTitleUseCase,
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
                command = EditFilmCommand(
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

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: UUID,
    ): FilmResponse =
        FilmResponse.fromDomain(getFilmByIdUseCase.getById(id))

    @GetMapping("/search")
    fun searchByTitle(
        @RequestParam title: String,
    ): FilmResponse? =
        searchFilmByTitleUseCase.searchByTitle(title)?.let { FilmResponse.fromDomain(it) }

    @GetMapping
    fun getAll(): List<FilmResponse> =
        getAllFilmsUseCase.getAll().map { FilmResponse.fromDomain(it) }
}
