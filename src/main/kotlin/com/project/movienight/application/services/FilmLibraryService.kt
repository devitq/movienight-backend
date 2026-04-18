package com.project.movienight.application.services

import com.project.movienight.application.ports.input.AddFilmToLibraryCommand
import com.project.movienight.application.ports.input.AddFilmToLibraryUseCase
import com.project.movienight.application.ports.input.CreateFilmLibraryCommand
import com.project.movienight.application.ports.input.CreateFilmLibraryUseCase
import com.project.movienight.application.ports.input.GetFilmLibraryQuery
import com.project.movienight.application.ports.input.GetFilmLibraryUseCase
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
) : CreateFilmLibraryUseCase,
    AddFilmToLibraryUseCase,
    RemoveFilmFromLibraryUseCase,
    GetFilmLibraryUseCase {
    override fun create(command: CreateFilmLibraryCommand): FilmLibrary {
        val existingLibrary = findByUserId(command.userId)
        if (existingLibrary != null) {
            return existingLibrary
        }

        return filmLibraryRepository.save(
            FilmLibrary(
                id = idGenerator.generateId(),
                userId = command.userId,
                filmId = idGenerator.generateId(),
                comment = command.name,
                isViewed = false,
            ),
        )
    }

    override fun addFilm(command: AddFilmToLibraryCommand): FilmLibrary {
        val existingLibrary = findByUserId(command.userId)
        if (existingLibrary == null) {
            return filmLibraryRepository.save(
                FilmLibrary(
                    id = idGenerator.generateId(),
                    userId = command.userId,
                    filmId = command.filmId,
                    comment = null,
                    isViewed = false,
                ),
            )
        }

        return filmLibraryRepository.save(
            existingLibrary.copy(
                filmId = command.filmId,
                isViewed = false,
            ),
        )
    }

    override fun removeFilm(command: RemoveFilmFromLibraryCommand): FilmLibrary {
        val existingLibrary =
            findByUserId(command.userId)
                ?: throw EntityNotFoundException(entity = "Film library", id = command.userId.toString())

        if (command.libraryId != null && command.libraryId != existingLibrary.id) {
            throw EntityNotFoundException(entity = "Film library", id = command.libraryId.toString())
        }

        if (existingLibrary.filmId != command.filmId) {
            throw DomainException("Film with id ${command.filmId} not found in user's library")
        }

        filmLibraryRepository.deleteById(existingLibrary.id)
        return existingLibrary
    }

    override fun getLibrary(query: GetFilmLibraryQuery): FilmLibrary =
        findByUserId(query.userId)
            ?: throw EntityNotFoundException(entity = "Film library", id = query.userId.toString())

    private fun findByUserId(userId: UUID): FilmLibrary? =
        filmLibraryRepository.findAll().firstOrNull { it.userId == userId }
}
