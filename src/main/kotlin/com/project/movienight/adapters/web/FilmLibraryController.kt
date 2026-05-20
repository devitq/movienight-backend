package com.project.movienight.adapters.web

import com.project.movienight.adapters.web.dto.request.CreateFilmLibraryRequest
import com.project.movienight.adapters.web.dto.response.FilmLibraryResponse
import com.project.movienight.adapters.web.dto.response.FilmResponse
import com.project.movienight.application.ports.input.AddFilmToLibraryCommand
import com.project.movienight.application.ports.input.AddFilmToLibraryUseCase
import com.project.movienight.application.ports.input.CreateFilmLibraryCommand
import com.project.movienight.application.ports.input.CreateFilmLibraryUseCase
import com.project.movienight.application.ports.input.GetAllFilmsUseCase
import com.project.movienight.application.ports.input.GetFilmByIdUseCase
import com.project.movienight.application.ports.input.GetFilmLibraryQuery
import com.project.movienight.application.ports.input.GetFilmLibraryUseCase
import com.project.movienight.application.ports.input.ListFilmLibraryEntriesUseCase
import com.project.movienight.application.ports.input.MarkFilmViewedCommand
import com.project.movienight.application.ports.input.MarkFilmViewedUseCase
import com.project.movienight.application.ports.input.RemoveFilmFromLibraryCommand
import com.project.movienight.application.ports.input.RemoveFilmFromLibraryUseCase
import com.project.movienight.domain.exception.EntityNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/users/{userId}/library")
class FilmLibraryController(
    private val createFilmLibraryUseCase: CreateFilmLibraryUseCase,
    private val addFilmToLibraryUseCase: AddFilmToLibraryUseCase,
    private val markFilmViewedUseCase: MarkFilmViewedUseCase,
    private val removeFilmFromLibraryUseCase: RemoveFilmFromLibraryUseCase,
    private val getFilmLibraryUseCase: GetFilmLibraryUseCase,
    private val listFilmLibraryEntriesUseCase: ListFilmLibraryEntriesUseCase,
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

    @GetMapping("/entries")
    fun list(
        @PathVariable userId: UUID,
    ): List<FilmLibraryResponse> = listFilmLibraryEntriesUseCase.list(userId).map { FilmLibraryResponse.fromDomain(it) }

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

    @PostMapping("/films/{filmId}/viewed")
    fun markViewed(
        @PathVariable userId: UUID,
        @PathVariable filmId: UUID,
    ): FilmLibraryResponse =
        FilmLibraryResponse.fromDomain(
            markFilmViewedUseCase.markViewed(
                MarkFilmViewedCommand(
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
    ) {
        removeFilmFromLibraryUseCase.removeFilm(
            RemoveFilmFromLibraryCommand(
                userId = userId,
                filmId = filmId,
            ),
        )
    }

    @GetMapping("/available-films")
    fun getAvailableFilms(
        @PathVariable userId: UUID,
    ): List<FilmResponse> {
        val userLibrary =
            runCatching {
                getFilmLibraryUseCase.getLibrary(
                    GetFilmLibraryQuery(userId = userId),
                )
            }.onFailure { exception ->
                if (exception !is EntityNotFoundException) {
                    throw exception
                }
            }.getOrNull()

        val allFilms = getAllFilmsUseCase.getAll()

        val availableFilms =
            if (userLibrary != null) {
                allFilms.filter { it.id != userLibrary.filmId }
            } else {
                allFilms
            }

        return availableFilms.map { FilmResponse.fromDomain(it) }
    }
}
