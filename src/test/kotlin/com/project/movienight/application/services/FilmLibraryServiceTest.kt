package com.project.movienight.application.services

import com.project.movienight.application.ports.input.AddFilmToLibraryCommand
import com.project.movienight.application.ports.input.CreateFilmLibraryCommand
import com.project.movienight.application.ports.input.GetFilmLibraryQuery
import com.project.movienight.application.ports.input.RemoveFilmFromLibraryCommand
import com.project.movienight.application.ports.output.FilmLibraryRepositoryPort
import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.domain.exception.DomainException
import com.project.movienight.domain.exception.EntityNotFoundException
import com.project.movienight.domain.model.FilmLibrary
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

class FilmLibraryServiceTest {
    private lateinit var filmLibraryRepository: FilmLibraryRepositoryPort
    private lateinit var idGenerator: IdGenerator
    private lateinit var filmLibraryService: FilmLibraryService

    @BeforeEach
    fun setup() {
        filmLibraryRepository = mockk()
        idGenerator = mockk()
        filmLibraryService = FilmLibraryService(filmLibraryRepository, idGenerator)
    }

    @Test
    fun `should create new film library when user has no library`() {
        val userId = UUID.randomUUID()
        val libraryId = UUID.randomUUID()
        val filmId = UUID.randomUUID()
        val command = CreateFilmLibraryCommand(userId = userId, name = "My Films")
        val expectedLibrary =
            FilmLibrary(
                id = libraryId,
                userId = userId,
                filmId = filmId,
                comment = "My Films",
                isViewed = false,
            )

        every { filmLibraryRepository.findAll() } returns emptyList()
        every { idGenerator.generateId() } returnsMany listOf(libraryId, filmId)
        every {
            filmLibraryRepository.save(
                match {
                    it.userId == userId && it.comment == "My Films" && it.isViewed == false
                },
            )
        } returns expectedLibrary

        val result = filmLibraryService.create(command)

        assertNotNull(result)
        assertEquals(libraryId, result.id)
        assertEquals(userId, result.userId)
        assertEquals(filmId, result.filmId)
        assertEquals("My Films", result.comment)

        verify(exactly = 1) { filmLibraryRepository.findAll() }
        verify(exactly = 2) { idGenerator.generateId() }
        verify(exactly = 1) { filmLibraryRepository.save(any()) }
    }

    @Test
    fun `should return existing library when user already has one`() {
        val userId = UUID.randomUUID()
        val existingLibrary =
            FilmLibrary(
                id = UUID.randomUUID(),
                userId = userId,
                filmId = UUID.randomUUID(),
                comment = "Existing Library",
                isViewed = false,
            )
        val command = CreateFilmLibraryCommand(userId = userId, name = "New Library")

        every { filmLibraryRepository.findAll() } returns listOf(existingLibrary)

        val result = filmLibraryService.create(command)

        assertEquals(existingLibrary, result)

        verify(exactly = 1) { filmLibraryRepository.findAll() }
        verify(exactly = 0) { idGenerator.generateId() }
        verify(exactly = 0) { filmLibraryRepository.save(any()) }
    }

    @Test
    fun `should add film to new library when user has no library`() {
        val userId = UUID.randomUUID()
        val filmId = UUID.randomUUID()
        val libraryId = UUID.randomUUID()
        val command = AddFilmToLibraryCommand(userId = userId, filmId = filmId)
        val expectedLibrary =
            FilmLibrary(
                id = libraryId,
                userId = userId,
                filmId = filmId,
                comment = null,
                isViewed = false,
            )

        every { filmLibraryRepository.findAll() } returns emptyList()
        every { idGenerator.generateId() } returns libraryId
        every {
            filmLibraryRepository.save(
                match {
                    it.userId == userId && it.filmId == filmId && it.comment == null && it.isViewed == false
                },
            )
        } returns expectedLibrary

        val result = filmLibraryService.addFilm(command)

        assertNotNull(result)
        assertEquals(filmId, result.filmId)
        assertEquals(userId, result.userId)

        verify(exactly = 1) { filmLibraryRepository.findAll() }
        verify(exactly = 1) { idGenerator.generateId() }
        verify(exactly = 1) { filmLibraryRepository.save(any()) }
    }

    @Test
    fun `should add film to existing library`() {
        val userId = UUID.randomUUID()
        val oldFilmId = UUID.randomUUID()
        val newFilmId = UUID.randomUUID()
        val existingLibrary =
            FilmLibrary(
                id = UUID.randomUUID(),
                userId = userId,
                filmId = oldFilmId,
                comment = "My Library",
                isViewed = true,
            )
        val command = AddFilmToLibraryCommand(userId = userId, filmId = newFilmId)
        val updatedLibrary = existingLibrary.copy(filmId = newFilmId, isViewed = false)

        every { filmLibraryRepository.findAll() } returns listOf(existingLibrary)
        every {
            filmLibraryRepository.save(
                match {
                    it.filmId == newFilmId && it.isViewed == false
                },
            )
        } returns updatedLibrary

        val result = filmLibraryService.addFilm(command)

        assertEquals(newFilmId, result.filmId)
        assertEquals(false, result.isViewed)

        verify(exactly = 1) { filmLibraryRepository.findAll() }
        verify(exactly = 0) { idGenerator.generateId() }
        verify(exactly = 1) { filmLibraryRepository.save(any()) }
    }

    @Test
    fun `should remove film from library successfully`() {
        val userId = UUID.randomUUID()
        val filmId = UUID.randomUUID()
        val libraryId = UUID.randomUUID()
        val existingLibrary =
            FilmLibrary(
                id = libraryId,
                userId = userId,
                filmId = filmId,
                comment = "My Library",
                isViewed = false,
            )
        val command = RemoveFilmFromLibraryCommand(userId = userId, filmId = filmId)

        every { filmLibraryRepository.findAll() } returns listOf(existingLibrary)
        justRun { filmLibraryRepository.deleteById(libraryId) }

        val result = filmLibraryService.removeFilm(command)

        assertEquals(existingLibrary, result)

        verify(exactly = 1) { filmLibraryRepository.findAll() }
        verify(exactly = 1) { filmLibraryRepository.deleteById(libraryId) }
    }

    @Test
    fun `should throw EntityNotFoundException when removing film from non-existent library`() {
        val userId = UUID.randomUUID()
        val filmId = UUID.randomUUID()
        val command = RemoveFilmFromLibraryCommand(userId = userId, filmId = filmId)

        every { filmLibraryRepository.findAll() } returns emptyList()

        assertThrows<EntityNotFoundException> {
            filmLibraryService.removeFilm(command)
        }

        verify(exactly = 1) { filmLibraryRepository.findAll() }
        verify(exactly = 0) { filmLibraryRepository.deleteById(any()) }
    }

    @Test
    fun `should throw DomainException when removing film that is not in library`() {
        val userId = UUID.randomUUID()
        val libraryFilmId = UUID.randomUUID()
        val differentFilmId = UUID.randomUUID()
        val existingLibrary =
            FilmLibrary(
                id = UUID.randomUUID(),
                userId = userId,
                filmId = libraryFilmId,
                comment = "My Library",
                isViewed = false,
            )
        val command = RemoveFilmFromLibraryCommand(userId = userId, filmId = differentFilmId)

        every { filmLibraryRepository.findAll() } returns listOf(existingLibrary)

        assertThrows<DomainException> {
            filmLibraryService.removeFilm(command)
        }

        verify(exactly = 1) { filmLibraryRepository.findAll() }
        verify(exactly = 0) { filmLibraryRepository.deleteById(any()) }
    }

    @Test
    fun `should throw EntityNotFoundException when libraryId does not match`() {
        val userId = UUID.randomUUID()
        val filmId = UUID.randomUUID()
        val actualLibraryId = UUID.randomUUID()
        val wrongLibraryId = UUID.randomUUID()
        val existingLibrary =
            FilmLibrary(
                id = actualLibraryId,
                userId = userId,
                filmId = filmId,
                comment = "My Library",
                isViewed = false,
            )
        val command =
            RemoveFilmFromLibraryCommand(
                userId = userId,
                filmId = filmId,
                libraryId = wrongLibraryId,
            )

        every { filmLibraryRepository.findAll() } returns listOf(existingLibrary)

        assertThrows<EntityNotFoundException> {
            filmLibraryService.removeFilm(command)
        }

        verify(exactly = 1) { filmLibraryRepository.findAll() }
        verify(exactly = 0) { filmLibraryRepository.deleteById(any()) }
    }

    @Test
    fun `should get library successfully`() {
        val userId = UUID.randomUUID()
        val existingLibrary =
            FilmLibrary(
                id = UUID.randomUUID(),
                userId = userId,
                filmId = UUID.randomUUID(),
                comment = "My Library",
                isViewed = false,
            )
        val query = GetFilmLibraryQuery(userId = userId)

        every { filmLibraryRepository.findAll() } returns listOf(existingLibrary)

        val result = filmLibraryService.getLibrary(query)

        assertEquals(existingLibrary, result)

        verify(exactly = 1) { filmLibraryRepository.findAll() }
    }

    @Test
    fun `should throw EntityNotFoundException when getting non-existent library`() {
        val userId = UUID.randomUUID()
        val query = GetFilmLibraryQuery(userId = userId)

        every { filmLibraryRepository.findAll() } returns emptyList()

        assertThrows<EntityNotFoundException> {
            filmLibraryService.getLibrary(query)
        }

        verify(exactly = 1) { filmLibraryRepository.findAll() }
    }
}
