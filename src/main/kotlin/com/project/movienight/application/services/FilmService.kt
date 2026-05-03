package com.project.movienight.application.services

import com.project.movienight.application.ports.input.CreateFilmCommand
import com.project.movienight.application.ports.input.CreateFilmUseCase
import com.project.movienight.application.ports.input.DeleteFilmUseCase
import com.project.movienight.application.ports.input.EditFilmCommand
import com.project.movienight.application.ports.input.EditFilmUseCase
import com.project.movienight.application.ports.input.GetAllFilmsUseCase
import com.project.movienight.application.ports.input.GetFilmByIdUseCase
import com.project.movienight.application.ports.input.SearchFilmByTitleUseCase
import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.config.FilmServiceProperties
import com.project.movienight.domain.exception.BlockedValueException
import com.project.movienight.domain.exception.EntityNotFoundException
import com.project.movienight.domain.model.Film
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FilmService(
    private val filmRepository: FilmRepositoryPort,
    private val idGenerator: IdGenerator,
    private val filmConfig: FilmServiceProperties,
) : CreateFilmUseCase,
    EditFilmUseCase,
    DeleteFilmUseCase,
    GetFilmByIdUseCase,
    GetAllFilmsUseCase,
    SearchFilmByTitleUseCase {

    override fun create(command: CreateFilmCommand): Film {
        if (filmConfig.isBlocked(command.title)) {
            throw BlockedValueException(target = "Film", field = "title")
        }
        if (filmConfig.isBlocked(command.description)) {
            throw BlockedValueException(target = "Film", field = "description")
        }

        val film = Film(
            id = idGenerator.generateId(),
            title = command.title,
            description = command.description,
        )
        return filmRepository.save(film)
    }

    override fun edit(id: UUID, command: EditFilmCommand): Film {
        if (filmConfig.isBlocked(command.title)) {
            throw BlockedValueException(target = "Film", field = "title")
        }
        if (filmConfig.isBlocked(command.description)) {
            throw BlockedValueException(target = "Film", field = "description")
        }

        var film = filmRepository.findById(id) ?: throw EntityNotFoundException(entity = "Film", id = id.toString())
        film = film.copy(title = command.title, description = command.description)
        return filmRepository.save(film)
    }

    override fun delete(id: UUID) {
        filmRepository.findById(id) ?: throw EntityNotFoundException(entity = "Film", id = id.toString())
        filmRepository.deleteById(id)
    }

    override fun getById(id: UUID): Film {
        return filmRepository.findById(id) ?: throw EntityNotFoundException(entity = "Film", id = id.toString())
    }

    override fun getAll(): List<Film> = filmRepository.findAll()

    override fun searchByTitle(title: String): Film? = filmRepository.findByTitle(title)
}
