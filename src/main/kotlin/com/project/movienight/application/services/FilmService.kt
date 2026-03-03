package com.project.movienight.application.services

import com.project.movienight.application.ports.input.CreateFilmCommand
import com.project.movienight.application.ports.input.CreateFilmUseCase
import com.project.movienight.application.ports.input.DeleteFilmUseCase
import com.project.movienight.application.ports.input.EditFilmCommand
import com.project.movienight.application.ports.input.EditFilmUseCase
import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.config.FilmServiceProperties
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
    DeleteFilmUseCase {
    override fun create(command: CreateFilmCommand): Film {
        if (filmConfig.isBlocked(command.title)) {
            throw IllegalArgumentException("Film with this title is not acceptable")
        }
        if (filmConfig.isBlocked(command.description)) {
            throw IllegalArgumentException("Film with this description is not acceptable")
        }

        val film =
            Film(
                id = idGenerator.generateId(),
                title = command.title,
                description = command.description,
            )
        return filmRepository.save(film)
    }

    override fun edit(
        id: UUID,
        command: EditFilmCommand,
    ): Film {
        if (filmConfig.isBlocked(command.title)) {
            throw IllegalArgumentException("Film with this title is not acceptable")
        }
        if (filmConfig.isBlocked(command.description)) {
            throw IllegalArgumentException("Film with this description is not acceptable")
        }

        var film = filmRepository.findById(id) ?: throw IllegalArgumentException("Film with id $id not found")

        film = film.copy(title = command.title, description = command.description)

        return filmRepository.save(film)
    }

    override fun delete(id: UUID) {
        filmRepository.findById(id) ?: throw IllegalArgumentException("Film with id $id not found")

        filmRepository.deleteById(id)
    }
}
