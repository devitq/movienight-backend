package com.project.movienight.application.services

import com.project.movienight.application.ports.input.AddFilmToLibraryCommand
import com.project.movienight.application.ports.input.FilmLibraryUseCase
import com.project.movienight.application.ports.input.MarkFilmViewedCommand
import com.project.movienight.application.ports.input.RemoveFilmFromLibraryCommand
import com.project.movienight.application.ports.output.BusinessMetricsPort
import com.project.movienight.application.ports.output.FilmLibraryEntryRepositoryPort
import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.domain.exception.DomainException
import com.project.movienight.domain.exception.EntityNotFoundException
import com.project.movienight.domain.model.Film
import com.project.movienight.domain.model.FilmLibraryEntry
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FilmLibraryService(
    private val filmLibraryEntryRepository: FilmLibraryEntryRepositoryPort,
    private val filmRepository: FilmRepositoryPort,
    private val idGenerator: IdGenerator,
    private val businessMetricsService: BusinessMetricsPort,
) : FilmLibraryUseCase {
    override fun addFilm(command: AddFilmToLibraryCommand): FilmLibraryEntry {
        ensureFilmExists(command.filmId)

        val existingEntry = filmLibraryEntryRepository.findByUserIdAndFilmId(command.userId, command.filmId)
        if (existingEntry != null) {
            val saved =
                filmLibraryEntryRepository.save(
                    existingEntry.copy(
                        isViewed = false,
                        watchedAt = null,
                    ),
                )
            businessMetricsService.recordLibraryEvent()
            return saved
        }

        val saved =
            filmLibraryEntryRepository.save(
                FilmLibraryEntry(
                    id = idGenerator.generateId(),
                    userId = command.userId,
                    filmId = command.filmId,
                    comment = null,
                    isViewed = false,
                    watchedAt = null,
                ),
            )
        businessMetricsService.recordLibraryEvent()
        return saved
    }

    override fun removeFilm(command: RemoveFilmFromLibraryCommand): FilmLibraryEntry {
        val existingEntry =
            if (command.entryId != null) {
                filmLibraryEntryRepository.findById(command.entryId)
                    ?: throw EntityNotFoundException(entity = "Film library entry", id = command.entryId.toString())
            } else {
                filmLibraryEntryRepository.findByUserIdAndFilmId(command.userId, command.filmId)
                    ?: throw EntityNotFoundException(entity = "Film library entry", id = command.filmId.toString())
            }

        if (existingEntry.userId != command.userId || existingEntry.filmId != command.filmId) {
            throw DomainException("Film with id ${command.filmId} not found in user's library")
        }

        filmLibraryEntryRepository.deleteById(existingEntry.id)
        businessMetricsService.recordLibraryEvent()
        return existingEntry
    }

    override fun markViewed(command: MarkFilmViewedCommand): FilmLibraryEntry {
        ensureFilmExists(command.filmId)

        val existingEntry = filmLibraryEntryRepository.findByUserIdAndFilmId(command.userId, command.filmId)
        val watchedAt = command.watchedAt ?: java.time.LocalDateTime.now()

        val saved =
            if (existingEntry == null) {
                filmLibraryEntryRepository.save(
                    FilmLibraryEntry(
                        id = idGenerator.generateId(),
                        userId = command.userId,
                        filmId = command.filmId,
                        comment = null,
                        isViewed = true,
                        watchedAt = watchedAt,
                    ),
                )
            } else {
                filmLibraryEntryRepository.save(
                    existingEntry.copy(
                        isViewed = true,
                        watchedAt = watchedAt,
                    ),
                )
            }
        businessMetricsService.recordLibraryEvent()
        return saved
    }

    override fun list(userId: UUID): List<FilmLibraryEntry> = filmLibraryEntryRepository.findByUserId(userId)

    override fun listAvailableFilms(userId: UUID): List<Film> {
        val libraryFilmIds = list(userId).map { it.filmId }.toSet()
        return filmRepository.findAll().filter { it.id !in libraryFilmIds }
    }

    private fun ensureFilmExists(filmId: UUID) {
        filmRepository.findById(filmId) ?: throw EntityNotFoundException(entity = "Film", id = filmId.toString())
    }
}
