package com.project.movienight.adapters.web

import com.project.movienight.adapters.web.dto.request.CreateFilmLibraryRequest
import com.project.movienight.adapters.web.dto.response.FilmLibraryResponse
import com.project.movienight.adapters.web.dto.response.FilmResponse
import com.project.movienight.application.ports.input.AddFilmToLibraryCommand
import com.project.movienight.application.ports.input.AddFilmToLibraryUseCase
import com.project.movienight.application.ports.input.CreateFilmLibraryCommand
import com.project.movienight.application.ports.input.CreateFilmLibraryUseCase
import com.project.movienight.application.ports.input.GetFilmLibraryQuery
import com.project.movienight.application.ports.input.GetFilmLibraryUseCase
import com.project.movienight.application.ports.input.RemoveFilmFromLibraryCommand
import com.project.movienight.application.ports.input.RemoveFilmFromLibraryUseCase
import com.project.movienight.application.services.FilmService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/users/{userId}/library")
class FilmLibraryController(
    private val createFilmLibraryUseCase: CreateFilmLibraryUseCase,
    private val addFilmToLibraryUseCase: AddFilmToLibraryUseCase,
    private val removeFilmFromLibraryUseCase: RemoveFilmFromLibraryUseCase,
    private val getFilmLibraryUseCase: GetFilmLibraryUseCase,
    private val filmService: FilmService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable userId: UUID,
        @RequestBody request: CreateFilmLibraryRequest,
    ): FilmLibraryResponse =
        FilmLibraryResponse.fromDomain(
            createFilmLibraryUseCase.create(
                CreateFilmLibraryCommand(
                    userId = userId,
                    name = request.name,
                ),
            ),
        )

    @GetMapping
    fun get(
        @PathVariable userId: UUID,
    ): FilmLibraryResponse =
        FilmLibraryResponse.fromDomain(
            getFilmLibraryUseCase.getLibrary(
                GetFilmLibraryQuery(userId = userId),
            ),
        )

    @PostMapping("/films/{filmId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun addFilm(
        @PathVariable userId: UUID,
        @PathVariable filmId: UUID,
    ): FilmLibraryResponse =
        FilmLibraryResponse.fromDomain(
            addFilmToLibraryUseCase.addFilm(
                AddFilmToLibraryCommand(
                    userId = userId,
                    filmId = filmId,
                ),
            ),
        )

    @DeleteMapping("/films/{filmId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeFilm(
        @PathVariable userId: UUID,
        @PathVariable filmId: UUID,
    ) = removeFilmFromLibraryUseCase.removeFilm(
        RemoveFilmFromLibraryCommand(
            userId = userId,
            filmId = filmId,
        ),
    )

    @GetMapping("/available-films")
    fun getAvailableFilms(
        @PathVariable userId: UUID,
    ): List<FilmResponse> {
        val userLibrary = getFilmLibraryUseCase.getLibrary(
            GetFilmLibraryQuery(userId = userId)
        )

        val allFilms = filmService.findAll()

        val availableFilms = allFilms.filter { it.id != userLibrary.filmId }

        return availableFilms.map { FilmResponse.fromDomain(it) }
    }
}
