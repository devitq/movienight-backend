package com.project.movienight.application.services

import com.project.movienight.adapters.metrics.BusinessMetricsService
import com.project.movienight.application.ports.input.AddFilmToLibraryCommand
import com.project.movienight.application.ports.input.AddFilmToLibraryUseCase
import com.project.movienight.application.ports.input.CreateFilmLibraryCommand
import com.project.movienight.application.ports.input.CreateFilmLibraryUseCase
import com.project.movienight.application.ports.input.GetFilmLibraryQuery
import com.project.movienight.application.ports.input.GetFilmLibraryUseCase
import com.project.movienight.application.ports.input.ListFilmLibraryEntriesUseCase
import com.project.movienight.application.ports.input.MarkFilmViewedCommand
import com.project.movienight.application.ports.input.MarkFilmViewedUseCase
import com.project.movienight.application.ports.input.RemoveFilmFromLibraryCommand
import com.project.movienight.application.ports.input.RemoveFilmFromLibraryUseCase
import com.project.movienight.application.ports.output.FilmLibraryRepositoryPort
import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.domain.exception.DomainException
import com.project.movienight.domain.exception.EntityNotFoundException
import com.project.movienight.domain.model.FilmLibrary
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FilmLibraryService(
    private val filmLibraryRepository: FilmLibraryRepositoryPort,
    private val idGenerator: IdGenerator,
    private val businessMetricsService: BusinessMetricsService,
) : CreateFilmLibraryUseCase,
    AddFilmToLibraryUseCase,
    MarkFilmViewedUseCase,
    RemoveFilmFromLibraryUseCase,
    GetFilmLibraryUseCase,
    ListFilmLibraryEntriesUseCase {
    override fun create(command: CreateFilmLibraryCommand): FilmLibrary {
        findByUserId(command.userId)?.let { return it }
        throw EntityNotFoundException(entity = "Film library", id = command.userId.toString())
    }

    override fun addFilm(command: AddFilmToLibraryCommand): FilmLibrary {
        val existingEntry = findByUserAndFilmId(command.userId, command.filmId)
        if (existingEntry != null) {
            val saved =
                filmLibraryRepository.save(
                    existingEntry.copy(
                        isViewed = false,
                        watchedAt = null,
                    ),
                )
            businessMetricsService.recordLibraryEvent()
            return saved
        }

        val saved =
            filmLibraryRepository.save(
                FilmLibrary(
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

    override fun removeFilm(command: RemoveFilmFromLibraryCommand): FilmLibrary {
        val existingLibrary =
            if (command.libraryId != null) {
                filmLibraryRepository.findById(command.libraryId)
                    ?: throw EntityNotFoundException(entity = "Film library", id = command.libraryId.toString())
            } else {
                findByUserAndFilmId(command.userId, command.filmId)
                    ?: throw EntityNotFoundException(entity = "Film library", id = command.filmId.toString())
            }

        if (existingLibrary.userId != command.userId || existingLibrary.filmId != command.filmId) {
            throw DomainException("Film with id ${command.filmId} not found in user's library")
        }

        filmLibraryRepository.deleteById(existingLibrary.id)
        businessMetricsService.recordLibraryEvent()
        return existingLibrary
    }

    override fun markViewed(command: MarkFilmViewedCommand): FilmLibrary {
        val existingEntry = findByUserAndFilmId(command.userId, command.filmId)
        val watchedAt = command.watchedAt ?: java.time.LocalDateTime.now()

        val saved =
            if (existingEntry == null) {
                filmLibraryRepository.save(
                    FilmLibrary(
                        id = idGenerator.generateId(),
                        userId = command.userId,
                        filmId = command.filmId,
                        comment = null,
                        isViewed = true,
                        watchedAt = watchedAt,
                    ),
                )
            } else {
                filmLibraryRepository.save(
                    existingEntry.copy(
                        isViewed = true,
                        watchedAt = watchedAt,
                    ),
                )
            }
        businessMetricsService.recordLibraryEvent()
        return saved
    }

    override fun getLibrary(query: GetFilmLibraryQuery): FilmLibrary =
        findByUserId(query.userId)
            ?: throw EntityNotFoundException(entity = "Film library", id = query.userId.toString())

    override fun list(userId: UUID): List<FilmLibrary> = filmLibraryRepository.findAll().filter { it.userId == userId }

    private fun findByUserId(userId: UUID): FilmLibrary? =
        filmLibraryRepository.findAll().firstOrNull { it.userId == userId }

    private fun findByUserAndFilmId(
        userId: UUID,
        filmId: UUID,
    ): FilmLibrary? = filmLibraryRepository.findAll().firstOrNull { it.userId == userId && it.filmId == filmId }
}
