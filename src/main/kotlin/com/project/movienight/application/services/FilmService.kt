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
        log.info("Creating new film: title='{}', contentType={}", command.title, command.contentType)
        log.debug("Create film request details: title='{}', descriptionLength={}, genres={}, releaseYear={}",
            command.title, command.description.length, command.genres, command.releaseYear)

        val sample = Timer.start(meterRegistry)

        try {
            if (filmConfig.isBlocked(command.title)) {
                log.warn("Film creation blocked: title contains blocked pattern '{}'", command.title)
                filmBlockedCounter.increment()
                throw BlockedValueException(target = "Film", field = "title")
            }
            if (filmConfig.isBlocked(command.description)) {
                log.warn("Film creation blocked: description contains blocked pattern")
                filmBlockedCounter.increment()
                throw BlockedValueException(target = "Film", field = "description")
            }

            val film = Film(
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
            log.info("Film created successfully: id={}, title='{}'", saved.id, saved.title)
            return saved
        } finally {
            sample.stop(createFilmTimer)
        }
    }

    override fun edit(id: UUID, command: EditFilmCommand): Film {
        log.info("Editing film: id={}", id)
        log.debug("Edit film request details: id={}, title='{}', descriptionLength={}, genres={}",
            id, command.title, command.description.length, command.genres)

        val sample = Timer.start(meterRegistry)

        try {
            if (filmConfig.isBlocked(command.title)) {
                log.warn("Film edit blocked: title contains blocked pattern '{}'", command.title)
                filmBlockedCounter.increment()
                throw BlockedValueException(target = "Film", field = "title")
            }
            if (filmConfig.isBlocked(command.description)) {
                log.warn("Film edit blocked: description contains blocked pattern")
                filmBlockedCounter.increment()
                throw BlockedValueException(target = "Film", field = "description")
            }

            var film = filmRepository.findById(id)

            if (film == null) {
                log.warn("Film not found for edit: id='{}'", id)
                throw EntityNotFoundException(entity = "Film", id = id.toString())
            }

            log.debug("Existing film found: id={}, current title='{}'", film.id, film.title)

            film = film.copy(
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
            log.info("Film edited successfully: id={}, new title='{}'", saved.id, saved.title)
            return saved
        } finally {
            sample.stop(editFilmTimer)
        }
    }

    override fun delete(id: UUID) {
        log.info("Deleting film: id={}", id)

        val sample = Timer.start(meterRegistry)

        try {
            val film = filmRepository.findById(id)

            if (film == null) {
                log.warn("Film not found for delete: id='{}'", id)
                throw EntityNotFoundException(entity = "Film", id = id.toString())
            }

            log.debug("Film found for deletion: id={}, title='{}'", film.id, film.title)

            filmRepository.deleteById(id)
            filmDeletedCounter.increment()
            log.info("Film deleted successfully: id={}, title='{}'", id, film.title)
        } finally {
            sample.stop(deleteFilmTimer)
        }
    }

    override fun getById(id: UUID): Film {
        log.debug("Fetching film by id: {}", id)
        val film = filmRepository.findById(id)
            ?: throw EntityNotFoundException(entity = "Film", id = id.toString())
        log.debug("Film found: id={}, title='{}'", film.id, film.title)
        return film
    }

    override fun getAll(): List<Film> {
        log.debug("Fetching all films")
        val films = filmRepository.findAll()
        log.info("Retrieved {} films from database", films.size)
        return films
    }

    override fun searchByTitle(title: String): Film? {
        log.debug("Searching film by title: '{}'", title)
        val film = filmRepository.findByTitle(title)
        if (film != null) {
            log.info("Film found by title '{}': id={}", title, film.id)
        } else {
            log.debug("No film found with title: '{}'", title)
        }
        return film
    }

    private val filmCreatedCounter = Counter
        .builder("film_created_total")
        .description("Total number of created films")
        .register(meterRegistry)

    private val filmEditedCounter = Counter
        .builder("film_edited_total")
        .description("Total number of successfully edited films")
        .register(meterRegistry)

    private val filmDeletedCounter = Counter
        .builder("film_deleted_total")
        .description("Total number of successfully deleted films")
        .register(meterRegistry)

    private val filmBlockedCounter = Counter
        .builder("films.blocked")
        .description("Total blocked film operations")
        .register(meterRegistry)

    private val createFilmTimer = Timer
        .builder("films.create.duration")
        .description("Film creation duration")
        .register(meterRegistry)

    private val editFilmTimer = Timer
        .builder("films.edit.duration")
        .description("Film edit duration")
        .register(meterRegistry)

    private val deleteFilmTimer = Timer
        .builder("films.delete.duration")
        .description("Film deletion duration")
        .register(meterRegistry)
}
