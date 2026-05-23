package com.project.movienight.adapters.web

import com.project.movienight.adapters.web.dto.response.FilmLibraryEntryResponse
import com.project.movienight.adapters.web.dto.response.FilmResponse
import com.project.movienight.application.ports.input.AddFilmToLibraryCommand
import com.project.movienight.application.ports.input.FilmLibraryUseCase
import com.project.movienight.application.ports.input.MarkFilmViewedCommand
import com.project.movienight.application.ports.input.RemoveFilmFromLibraryCommand
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/users/{userId}/library")
class FilmLibraryController(
    private val filmLibraryUseCase: FilmLibraryUseCase,
) {
    @GetMapping
    fun list(
        @PathVariable userId: UUID,
    ): List<FilmLibraryEntryResponse> =
        filmLibraryUseCase
            .list(userId)
            .map { entry -> FilmLibraryEntryResponse.fromDomain(entry) }

    @GetMapping("/entries")
    fun listEntries(
        @PathVariable userId: UUID,
    ): List<FilmLibraryEntryResponse> = list(userId)

    @PostMapping("/films/{filmId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun addFilm(
        @PathVariable userId: UUID,
        @PathVariable filmId: UUID,
    ): FilmLibraryEntryResponse =
        FilmLibraryEntryResponse.fromDomain(
            filmLibraryUseCase.addFilm(
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
    ): FilmLibraryEntryResponse =
        FilmLibraryEntryResponse.fromDomain(
            filmLibraryUseCase.markViewed(
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
        filmLibraryUseCase.removeFilm(
            RemoveFilmFromLibraryCommand(
                userId = userId,
                filmId = filmId,
            ),
        )
    }

    @GetMapping("/available-films")
    fun getAvailableFilms(
        @PathVariable userId: UUID,
    ): List<FilmResponse> = filmLibraryUseCase.listAvailableFilms(userId).map { FilmResponse.fromDomain(it) }
}
