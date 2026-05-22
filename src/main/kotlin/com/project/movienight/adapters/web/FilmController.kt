package com.project.movienight.adapters.web

import com.project.movienight.adapters.web.dto.request.CreateFilmRequest
import com.project.movienight.adapters.web.dto.request.EditFilmRequest
import com.project.movienight.adapters.web.dto.response.FilmResponse
import com.project.movienight.application.ports.input.CreateFilmCommand
import com.project.movienight.application.ports.input.EditFilmCommand
import com.project.movienight.application.ports.input.FilmUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
    private val filmUseCase: FilmUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateFilmRequest,
    ): FilmResponse =
        FilmResponse.fromDomain(
            filmUseCase.create(
                CreateFilmCommand(
                    title = request.title,
                    description = request.description,
                    contentType = parseContentType(request.contentType),
                    releaseYear = request.releaseYear,
                    genres = request.genres,
                    cast = request.cast,
                    directors = request.directors,
                    imdbRating = request.imdbRating,
                    platformRating = request.platformRating,
                    externalUrl = request.externalUrl,
                    jellyfinItemId = request.jellyfinItemId,
                    jellyfinLibraryId = request.jellyfinLibraryId,
                ),
            ),
        )

    @PatchMapping("/{id}")
    fun edit(
        @PathVariable id: UUID,
        @Valid @RequestBody request: EditFilmRequest,
    ): FilmResponse =
        FilmResponse.fromDomain(
            filmUseCase.edit(
                id = id,
                command =
                    EditFilmCommand(
                        title = request.title,
                        description = request.description,
                        contentType = parseContentType(request.contentType),
                        releaseYear = request.releaseYear,
                        genres = request.genres,
                        cast = request.cast,
                        directors = request.directors,
                        imdbRating = request.imdbRating,
                        platformRating = request.platformRating,
                        externalUrl = request.externalUrl,
                        jellyfinItemId = request.jellyfinItemId,
                        jellyfinLibraryId = request.jellyfinLibraryId,
                    ),
            ),
        )

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: UUID,
    ) = filmUseCase.delete(id)

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: UUID,
    ): FilmResponse = FilmResponse.fromDomain(filmUseCase.getById(id))

    @GetMapping
    fun getAll(): List<FilmResponse> = filmUseCase.getAll().map { FilmResponse.fromDomain(it) }

    @GetMapping("/search")
    fun searchByTitle(
        @RequestParam title: String,
    ): ResponseEntity<FilmResponse> {
        val film = filmUseCase.searchByTitle(title)
        return if (film != null) {
            ResponseEntity.ok(FilmResponse.fromDomain(film))
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
