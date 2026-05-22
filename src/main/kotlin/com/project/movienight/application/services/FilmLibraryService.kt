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
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(javaClass)

    override fun create(command: CreateFilmLibraryCommand): FilmLibrary {
        log.info("Creating film library for user: {}", command.userId)
        log.debug("Create library request: userId={}, name={}", command.userId, command.name)

        val existing = findByUserId(command.userId)
        if (existing != null) {
            log.debug("Library already exists for user {}: libraryId={}", command.userId, existing.id)
            return existing
        }

        log.warn("Library not found for user {}, cannot create", command.userId)
        throw EntityNotFoundException(entity = "Film library", id = command.userId.toString())
    }

    override fun addFilm(command: AddFilmToLibraryCommand): FilmLibrary {
        log.info("Adding film to library: userId={}, filmId={}", command.userId, command.filmId)

        val existingEntry = findByUserAndFilmId(command.userId, command.filmId)
        if (existingEntry != null) {
            log.debug("Film already in library, resetting as not viewed: entryId={}", existingEntry.id)
            val saved =
                filmLibraryRepository.save(
                    existingEntry.copy(
                        isViewed = false,
                        watchedAt = null,
                    ),
                )
            businessMetricsService.recordLibraryEvent()
            log.info(
                "Film re-added to library: userId={}, filmId={}, entryId={}",
                command.userId,
                command.filmId,
                saved.id,
            )
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
        log.info(
            "Film added to library: userId={}, filmId={}, entryId={}",
            command.userId,
            command.filmId,
            saved.id,
        )
        return saved
    }

    override fun removeFilm(command: RemoveFilmFromLibraryCommand): FilmLibrary {
        log.info("Removing film from library: userId={}, filmId={}", command.userId, command.filmId)

        val existingLibrary =
            if (command.libraryId != null) {
                log.debug("Looking up by libraryId: {}", command.libraryId)
                filmLibraryRepository.findById(command.libraryId)
                    ?: throw EntityNotFoundException(entity = "Film library", id = command.libraryId.toString())
            } else {
                log.debug("Looking up by userId and filmId")
                findByUserAndFilmId(command.userId, command.filmId)
                    ?: throw EntityNotFoundException(entity = "Film library", id = command.filmId.toString())
            }

        if (existingLibrary.userId != command.userId || existingLibrary.filmId != command.filmId) {
            log.warn("Film not found in user's library: userId={}, filmId={}", command.userId, command.filmId)
            throw DomainException("Film with id ${command.filmId} not found in user's library")
        }

        filmLibraryRepository.deleteById(existingLibrary.id)
        businessMetricsService.recordLibraryEvent()
        log.info(
            "Film removed from library: userId={}, filmId={}, entryId={}",
            command.userId,
            command.filmId,
            existingLibrary.id,
        )
        return existingLibrary
    }

    override fun markViewed(command: MarkFilmViewedCommand): FilmLibrary {
        log.info("Marking film as viewed: userId={}, filmId={}", command.userId, command.filmId)

        val existingEntry = findByUserAndFilmId(command.userId, command.filmId)
        val watchedAt = command.watchedAt ?: java.time.LocalDateTime.now()

        log.debug("Marking as viewed at: {}", watchedAt)

        val saved =
            if (existingEntry == null) {
                log.debug("Film not in library, creating new entry as viewed")
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
                log.debug(
                    "Updating existing entry: entryId={}, was viewed={}",
                    existingEntry.id,
                    existingEntry.isViewed,
                )
                filmLibraryRepository.save(
                    existingEntry.copy(
                        isViewed = true,
                        watchedAt = watchedAt,
                    ),
                )
            }
        businessMetricsService.recordLibraryEvent()
        log.info(
            "Film marked as viewed: userId={}, filmId={}, entryId={}",
            command.userId,
            command.filmId,
            saved.id,
        )
        return saved
    }

    override fun getLibrary(query: GetFilmLibraryQuery): FilmLibrary {
        log.debug("Getting library for user: {}", query.userId)
        val library =
            findByUserId(query.userId)
                ?: throw EntityNotFoundException(entity = "Film library", id = query.userId.toString())
        log.debug("Library found: userId={}, libraryId={}", query.userId, library.id)
        return library
    }

    override fun list(userId: UUID): List<FilmLibrary> {
        log.debug("Listing all library entries for user: {}", userId)
        val entries = filmLibraryRepository.findAll().filter { it.userId == userId }
        log.info("User {} has {} films in library", userId, entries.size)
        return entries
    }

    private fun findByUserId(userId: UUID): FilmLibrary? =
        filmLibraryRepository.findAll().firstOrNull {
            it.userId == userId
        }

    private fun findByUserAndFilmId(
        userId: UUID,
        filmId: UUID,
    ): FilmLibrary? =
        filmLibraryRepository.findAll().firstOrNull {
            it.userId == userId && it.filmId == filmId
        }
}
