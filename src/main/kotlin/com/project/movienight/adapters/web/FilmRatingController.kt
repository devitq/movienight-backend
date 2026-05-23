package com.project.movienight.adapters.web

import com.project.movienight.adapters.web.dto.request.RateFilmRequest
import com.project.movienight.adapters.web.dto.response.FilmRatingResponse
import com.project.movienight.application.ports.input.FilmRatingUseCase
import com.project.movienight.application.ports.input.RateFilmCommand
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/users/{userId}/ratings")
class FilmRatingController(
    private val filmRatingUseCase: FilmRatingUseCase,
) {
    @PostMapping("/films/{filmId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun rate(
        @PathVariable userId: UUID,
        @PathVariable filmId: UUID,
        @Valid @RequestBody request: RateFilmRequest,
    ): FilmRatingResponse =
        FilmRatingResponse.fromDomain(
            filmRatingUseCase.rate(
                RateFilmCommand(
                    userId = userId,
                    filmId = filmId,
                    score = request.score,
                    note = request.note,
                ),
            ),
        )

    @GetMapping
    fun list(
        @PathVariable userId: UUID,
    ): List<FilmRatingResponse> = filmRatingUseCase.getRatings(userId).map { FilmRatingResponse.fromDomain(it) }
}
