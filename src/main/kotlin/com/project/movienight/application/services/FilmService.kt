package com.project.movienight.application.services

import com.project.movienight.application.ports.input.CreateFilmCommand
import com.project.movienight.application.ports.input.EditFilmCommand
import com.project.movienight.application.ports.input.FilmUseCase
import com.project.movienight.application.ports.output.BusinessMetricsPort
import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.config.FilmServiceProperties
import com.project.movienight.domain.exception.BlockedValueException
import com.project.movienight.domain.exception.EntityNotFoundException
import com.project.movienight.domain.model.Film
import io.micrometer.core.annotation.Timed
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FilmService(
    private val filmRepository: FilmRepositoryPort,
    private val idGenerator: IdGenerator,
    private val filmConfig: FilmServiceProperties,
    private val businessMetricsService: BusinessMetricsPort,
) : FilmUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    @Timed(
        value = "business_films_create_duration_seconds",
        description = "Film creation duration",
    )
    override fun create(command: CreateFilmCommand): Film {
        log.debug(
            "Create film request received: title='{}', descriptionLength={}",
            command.title,
            command.description.length,
        )

        if (filmConfig.isBlocked(command.title)) {
            log.debug("Create film blocked by title policy: title='{}'", command.title)
            businessMetricsService.recordFilmBlocked()
            throw BlockedValueException(target = "Film", field = "title")
        }
        if (filmConfig.isBlocked(command.description)) {
            log.debug("Create film blocked by description policy")
            businessMetricsService.recordFilmBlocked()
            throw BlockedValueException(target = "Film", field = "description")
        }

        val film =
            Film(
                id = idGenerator.generateId(),
                title = command.title,
                description = command.description,
                contentType = command.contentType,
                releaseYear = command.releaseYear,
                genres = command.genres,
                cast = command.cast,
                directors = command.directors,
                imdbRating = command.imdbRating,
                platformRating = command.platformRating,
                externalUrl = command.externalUrl,
                jellyfinItemId = command.jellyfinItemId,
                jellyfinLibraryId = command.jellyfinLibraryId,
            )

        val saved = filmRepository.save(film)
        businessMetricsService.recordFilmCreated()
        return saved
    }

    @Timed(
        value = "business_films_edit_duration_seconds",
        description = "Film edit duration",
    )
    override fun edit(
        id: UUID,
        command: EditFilmCommand,
    ): Film {
        log.debug("Edit film with id: {}", id)

        if (filmConfig.isBlocked(command.title)) {
            log.debug("Edit film blocked by title policy: title='{}'", command.title)
            businessMetricsService.recordFilmBlocked()
            throw BlockedValueException(target = "Film", field = "title")
        }
        if (filmConfig.isBlocked(command.description)) {
            log.debug("Edit film blocked by description policy")
            businessMetricsService.recordFilmBlocked()
            throw BlockedValueException(target = "Film", field = "description")
        }

        val film =
            filmRepository.findById(id)
                ?: throw EntityNotFoundException(entity = "Film", id = id.toString())

        val saved =
            filmRepository.save(
                film.copy(
                    title = command.title,
                    description = command.description,
                    contentType = command.contentType,
                    releaseYear = command.releaseYear,
                    genres = command.genres,
                    cast = command.cast,
                    directors = command.directors,
                    imdbRating = command.imdbRating,
                    platformRating = command.platformRating,
                    externalUrl = command.externalUrl,
                    jellyfinItemId = command.jellyfinItemId,
                    jellyfinLibraryId = command.jellyfinLibraryId,
                ),
            )
        businessMetricsService.recordFilmEdited()
        return saved
    }

    @Timed(
        value = "business_films_delete_duration_seconds",
        description = "Film deletion duration",
    )
    override fun delete(id: UUID) {
        log.debug("Delete film with id: {}", id)

        val film = filmRepository.findById(id)

        if (film == null) {
            log.debug("Film not found for delete: id='{}'", id)
            throw EntityNotFoundException(entity = "Film", id = id.toString())
        }

        filmRepository.deleteById(id)
        businessMetricsService.recordFilmDeleted()

        log.info("Film deleted: id='{}'", id)
    }

    override fun getById(id: UUID): Film =
        filmRepository.findById(id) ?: throw EntityNotFoundException(entity = "Film", id = id.toString())

    override fun getAll(): List<Film> = filmRepository.findAll()

    override fun searchByTitle(title: String): Film? = filmRepository.findByTitle(title)
}
