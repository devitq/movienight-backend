package com.project.movienight.application.services

import com.project.movienight.application.ports.input.CreateFilmCommand
import com.project.movienight.application.ports.input.EditFilmCommand
import com.project.movienight.application.ports.output.BusinessMetricsPort
import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.config.FilmServiceProperties
import com.project.movienight.domain.exception.BlockedValueException
import com.project.movienight.domain.exception.EntityNotFoundException
import com.project.movienight.domain.model.Film
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class FilmServiceTest {
    private lateinit var filmRepository: FilmRepositoryPort
    private lateinit var idGenerator: IdGenerator
    private lateinit var filmConfig: FilmServiceProperties
    private lateinit var businessMetricsService: BusinessMetricsPort
    private lateinit var filmService: FilmService

    @BeforeEach
    fun setup() {
        filmRepository = mockk()
        idGenerator = mockk()
        filmConfig = mockk()
        businessMetricsService = mockk(relaxed = true)
        filmService = FilmService(filmRepository, idGenerator, filmConfig, businessMetricsService)
    }

    @Test
    fun `should create film successfully`() {
        val command = CreateFilmCommand(title = "Inception", description = "A mind-bending thriller")
        val filmId = UUID.randomUUID()
        val expectedFilm = Film(id = filmId, title = "Inception", description = "A mind-bending thriller")

        every { filmConfig.isBlocked("Inception") } returns false
        every { filmConfig.isBlocked("A mind-bending thriller") } returns false
        every { idGenerator.generateId() } returns filmId
        every { filmRepository.save(any()) } returns expectedFilm

        val result = filmService.create(command)

        assertNotNull(result)
        assertEquals(filmId, result.id)
        assertEquals("Inception", result.title)
        assertEquals("A mind-bending thriller", result.description)

        verify(exactly = 1) { filmConfig.isBlocked("Inception") }
        verify(exactly = 1) { filmConfig.isBlocked("A mind-bending thriller") }
        verify(exactly = 1) { idGenerator.generateId() }
        verify(exactly = 1) { filmRepository.save(any()) }
    }

    @Test
    fun `should throw BlockedValueException when creating film with blocked title`() {
        val command = CreateFilmCommand(title = "censored", description = "Some description")

        every { filmConfig.isBlocked("censored") } returns true
        every { filmConfig.isBlocked("Some description") } returns false

        assertThrows<BlockedValueException> {
            filmService.create(command)
        }

        verify(exactly = 1) { filmConfig.isBlocked("censored") }
        verify(exactly = 0) { filmConfig.isBlocked("Some description") }
        verify(exactly = 0) { idGenerator.generateId() }
        verify(exactly = 0) { filmRepository.save(any()) }
    }

    @Test
    fun `should throw BlockedValueException when creating film with blocked description`() {
        val command = CreateFilmCommand(title = "Good Film", description = "python")

        every { filmConfig.isBlocked("Good Film") } returns false
        every { filmConfig.isBlocked("python") } returns true

        assertThrows<BlockedValueException> {
            filmService.create(command)
        }

        verify(exactly = 1) { filmConfig.isBlocked("Good Film") }
        verify(exactly = 1) { filmConfig.isBlocked("python") }
        verify(exactly = 0) { idGenerator.generateId() }
        verify(exactly = 0) { filmRepository.save(any()) }
    }

    @Test
    fun `should edit film successfully`() {
        val filmId = UUID.randomUUID()
        val command = EditFilmCommand(title = "Inception 2", description = "The sequel")
        val existingFilm = Film(id = filmId, title = "Inception", description = "A mind-bending thriller")
        val updatedFilm = Film(id = filmId, title = "Inception 2", description = "The sequel")

        every { filmConfig.isBlocked("Inception 2") } returns false
        every { filmConfig.isBlocked("The sequel") } returns false
        every { filmRepository.findById(filmId) } returns existingFilm
        every { filmRepository.save(any()) } returns updatedFilm

        val result = filmService.edit(filmId, command)

        assertNotNull(result)
        assertEquals(filmId, result.id)
        assertEquals("Inception 2", result.title)
        assertEquals("The sequel", result.description)

        verify(exactly = 1) { filmConfig.isBlocked("Inception 2") }
        verify(exactly = 1) { filmConfig.isBlocked("The sequel") }
        verify(exactly = 1) { filmRepository.findById(filmId) }
        verify(exactly = 1) { filmRepository.save(any()) }
    }

    @Test
    fun `should throw BlockedValueException when editing film with blocked title`() {
        val filmId = UUID.randomUUID()
        val command = EditFilmCommand(title = "epstein", description = "Some description")

        every { filmConfig.isBlocked("epstein") } returns true

        assertThrows<BlockedValueException> {
            filmService.edit(filmId, command)
        }

        verify(exactly = 1) { filmConfig.isBlocked("epstein") }
        verify(exactly = 0) { filmRepository.findById(any()) }
        verify(exactly = 0) { filmRepository.save(any()) }
    }

    @Test
    fun `should throw EntityNotFoundException when editing non-existent film`() {
        val filmId = UUID.randomUUID()
        val command = EditFilmCommand(title = "New Title", description = "New Description")

        every { filmConfig.isBlocked("New Title") } returns false
        every { filmConfig.isBlocked("New Description") } returns false
        every { filmRepository.findById(filmId) } returns null

        assertThrows<EntityNotFoundException> {
            filmService.edit(filmId, command)
        }

        verify(exactly = 1) { filmConfig.isBlocked("New Title") }
        verify(exactly = 1) { filmConfig.isBlocked("New Description") }
        verify(exactly = 1) { filmRepository.findById(filmId) }
        verify(exactly = 0) { filmRepository.save(any()) }
    }

    @Test
    fun `should delete film successfully`() {
        val filmId = UUID.randomUUID()
        val existingFilm = Film(id = filmId, title = "Inception", description = "A mind-bending thriller")

        every { filmRepository.findById(filmId) } returns existingFilm
        justRun { filmRepository.deleteById(filmId) }

        filmService.delete(filmId)

        verify(exactly = 1) { filmRepository.findById(filmId) }
        verify(exactly = 1) { filmRepository.deleteById(filmId) }
    }

    @Test
    fun `should throw EntityNotFoundException when deleting non-existent film`() {
        val filmId = UUID.randomUUID()

        every { filmRepository.findById(filmId) } returns null

        assertThrows<EntityNotFoundException> {
            filmService.delete(filmId)
        }

        verify(exactly = 1) { filmRepository.findById(filmId) }
        verify(exactly = 0) { filmRepository.deleteById(any()) }
    }
}
