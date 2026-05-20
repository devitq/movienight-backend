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
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FilmService(
    private val filmRepository: FilmRepositoryPort,
    private val idGenerator: IdGenerator,
    private val filmConfig: FilmServiceProperties,
    private val meterRegistry: MeterRegistry,
) : CreateFilmUseCase,
    EditFilmUseCase,
    DeleteFilmUseCase,
    GetFilmByIdUseCase,
    GetAllFilmsUseCase,
    SearchFilmByTitleUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun create(command: CreateFilmCommand): Film {
        val sample = Timer.start(meterRegistry)

        try {
            log.debug(
                "Create film request received: title='{}', descriptionLength={}'",
                command.title,
                command.description.length,
            )

            if (filmConfig.isBlocked(command.title)) {
                log.debug("Create film blocked by title policy: title='{}'", command.title)
                filmBlockedCounter.increment()
                throw BlockedValueException(target = "Film", field = "title")
            }
            if (filmConfig.isBlocked(command.description)) {
                log.debug("Create film blocked by description policy")
                filmBlockedCounter.increment()
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
            filmCreatedCounter.increment()
            return saved
        } finally {
            sample.stop(createFilmTimer)
        }
    }

    override fun edit(
        id: UUID,
        command: EditFilmCommand,
    ): Film {
        val sample = Timer.start(meterRegistry)

        try {
            log.debug("Edit film with id: {}", id)

            if (filmConfig.isBlocked(command.title)) {
                log.debug("Edit film blocked by title policy: title='{}'", command.title)
                filmBlockedCounter.increment()
                throw BlockedValueException(target = "Film", field = "title")
            }
            if (filmConfig.isBlocked(command.description)) {
                log.debug("Edit film blocked by description policy")
                filmBlockedCounter.increment()
                throw BlockedValueException(target = "Film", field = "description")
            }

            var film = filmRepository.findById(id)

            if (film == null) {
                log.debug("Film not found for edit: id='{}'", id)
                throw EntityNotFoundException(entity = "Film", id = id.toString())
            }

            film =
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
                )

            val saved = filmRepository.save(film)
            filmEditedCounter.increment()
            return saved
        } finally {
            sample.stop(editFilmTimer)
        }
    }

    override fun delete(id: UUID) {
        val sample = Timer.start(meterRegistry)

        try {
            log.debug("Delete film with id: {}", id)

            val film = filmRepository.findById(id)

            if (film == null) {
                log.debug("Film not found for delete: id='{}'", id)
                throw EntityNotFoundException(entity = "Film", id = id.toString())
            }

            filmRepository.deleteById(id)

            filmDeletedCounter.increment()

            log.info("Film deleted: id='{}'", id)
        } finally {
            sample.stop(deleteFilmTimer)
        }
    }

    override fun getById(id: UUID): Film =
        filmRepository.findById(id) ?: throw EntityNotFoundException(entity = "Film", id = id.toString())

    override fun getAll(): List<Film> = filmRepository.findAll()

    override fun searchByTitle(title: String): Film? = filmRepository.findByTitle(title)

    private val filmCreatedCounter =
        Counter
            .builder("film_created_total")
            .description("Total number of created films")
            .register(meterRegistry)

    private val filmEditedCounter =
        Counter
            .builder("film_edited_total")
            .description("Total number of successfully edited films")
            .register(meterRegistry)

    private val filmDeletedCounter =
        Counter
            .builder("film_deleted_total")
            .description("Total number of successfully deleted films")
            .register(meterRegistry)

    private val filmBlockedCounter =
        Counter
            .builder("films.blocked")
            .description("Total blocked film operations")
            .register(meterRegistry)

    private val createFilmTimer =
        Timer
            .builder("films.create.duration")
            .description("Film creation duration")
            .register(meterRegistry)

    private val editFilmTimer =
        Timer
            .builder("films.edit.duration")
            .description("Film edit duration")
            .register(meterRegistry)

    private val deleteFilmTimer =
        Timer
            .builder("films.delete.duration")
            .description("Film deletion duration")
            .register(meterRegistry)
}
