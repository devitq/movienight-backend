package com.project.movienight.application.services

import com.project.movienight.application.ports.input.AddFilmToLibraryCommand
import com.project.movienight.application.ports.input.RemoveFilmFromLibraryCommand
import com.project.movienight.application.ports.output.BusinessMetricsPort
import com.project.movienight.application.ports.output.FilmLibraryEntryRepositoryPort
import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.domain.exception.DomainException
import com.project.movienight.domain.exception.EntityNotFoundException
import com.project.movienight.domain.model.Film
import com.project.movienight.domain.model.FilmLibraryEntry
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
    private lateinit var filmLibraryEntryRepository: FilmLibraryEntryRepositoryPort
    private lateinit var filmRepository: FilmRepositoryPort
    private lateinit var idGenerator: IdGenerator
    private lateinit var businessMetricsService: BusinessMetricsPort
    private lateinit var filmLibraryService: FilmLibraryService

    @BeforeEach
    fun setup() {
        filmLibraryEntryRepository = mockk()
        filmRepository = mockk()
        idGenerator = mockk()
        businessMetricsService = mockk(relaxed = true)
        filmLibraryService =
            FilmLibraryService(
                filmLibraryEntryRepository,
                filmRepository,
                idGenerator,
                businessMetricsService,
            )
    }

    @Test
    fun `should add film as new library entry`() {
        val userId = UUID.randomUUID()
        val filmId = UUID.randomUUID()
        val entryId = UUID.randomUUID()
        val command = AddFilmToLibraryCommand(userId = userId, filmId = filmId)
        val expectedEntry =
            FilmLibraryEntry(
                id = entryId,
                userId = userId,
                filmId = filmId,
                comment = null,
                isViewed = false,
            )

        every { filmRepository.findById(filmId) } returns Film(filmId, "Film", "Description")
        every { filmLibraryEntryRepository.findByUserIdAndFilmId(userId, filmId) } returns null
        every { idGenerator.generateId() } returns entryId
        every {
            filmLibraryEntryRepository.save(
                match {
                    it.userId == userId && it.filmId == filmId && it.comment == null && it.isViewed == false
                },
            )
        } returns expectedEntry

        val result = filmLibraryService.addFilm(command)

        assertNotNull(result)
        assertEquals(filmId, result.filmId)
        assertEquals(userId, result.userId)

        verify(exactly = 1) { filmLibraryEntryRepository.findByUserIdAndFilmId(userId, filmId) }
        verify(exactly = 1) { idGenerator.generateId() }
        verify(exactly = 1) { filmLibraryEntryRepository.save(any()) }
    }

    @Test
    fun `should reset viewed state when adding existing entry`() {
        val userId = UUID.randomUUID()
        val filmId = UUID.randomUUID()
        val existingEntry =
            FilmLibraryEntry(
                id = UUID.randomUUID(),
                userId = userId,
                filmId = filmId,
                comment = "My Library",
                isViewed = true,
            )
        val updatedEntry = existingEntry.copy(isViewed = false, watchedAt = null)

        every { filmLibraryEntryRepository.findByUserIdAndFilmId(userId, filmId) } returns existingEntry
        every { filmRepository.findById(filmId) } returns Film(filmId, "Film", "Description")
        every { filmLibraryEntryRepository.save(updatedEntry) } returns updatedEntry

        val result = filmLibraryService.addFilm(AddFilmToLibraryCommand(userId = userId, filmId = filmId))

        assertEquals(updatedEntry, result)

        verify(exactly = 1) { filmLibraryEntryRepository.findByUserIdAndFilmId(userId, filmId) }
        verify(exactly = 0) { idGenerator.generateId() }
        verify(exactly = 1) { filmLibraryEntryRepository.save(updatedEntry) }
    }

    @Test
    fun `should remove film from library successfully`() {
        val userId = UUID.randomUUID()
        val filmId = UUID.randomUUID()
        val entryId = UUID.randomUUID()
        val existingEntry =
            FilmLibraryEntry(
                id = entryId,
                userId = userId,
                filmId = filmId,
                comment = "My Library",
                isViewed = false,
            )
        val command = RemoveFilmFromLibraryCommand(userId = userId, filmId = filmId)

        every { filmLibraryEntryRepository.findByUserIdAndFilmId(userId, filmId) } returns existingEntry
        justRun { filmLibraryEntryRepository.deleteById(entryId) }

        val result = filmLibraryService.removeFilm(command)

        assertEquals(existingEntry, result)

        verify(exactly = 1) { filmLibraryEntryRepository.findByUserIdAndFilmId(userId, filmId) }
        verify(exactly = 1) { filmLibraryEntryRepository.deleteById(entryId) }
    }

    @Test
    fun `should throw EntityNotFoundException when removing non-existent entry`() {
        val userId = UUID.randomUUID()
        val filmId = UUID.randomUUID()
        val command = RemoveFilmFromLibraryCommand(userId = userId, filmId = filmId)

        every { filmLibraryEntryRepository.findByUserIdAndFilmId(userId, filmId) } returns null

        assertThrows<EntityNotFoundException> {
            filmLibraryService.removeFilm(command)
        }

        verify(exactly = 1) { filmLibraryEntryRepository.findByUserIdAndFilmId(userId, filmId) }
        verify(exactly = 0) { filmLibraryEntryRepository.deleteById(any()) }
    }

    @Test
    fun `should throw DomainException when entry id belongs to another film`() {
        val userId = UUID.randomUUID()
        val filmId = UUID.randomUUID()
        val entryId = UUID.randomUUID()
        val existingEntry =
            FilmLibraryEntry(
                id = entryId,
                userId = userId,
                filmId = UUID.randomUUID(),
                comment = "My Library",
                isViewed = false,
            )
        val command = RemoveFilmFromLibraryCommand(userId = userId, filmId = filmId, entryId = entryId)

        every { filmLibraryEntryRepository.findById(entryId) } returns existingEntry

        assertThrows<DomainException> {
            filmLibraryService.removeFilm(command)
        }

        verify(exactly = 1) { filmLibraryEntryRepository.findById(entryId) }
        verify(exactly = 0) { filmLibraryEntryRepository.deleteById(any()) }
    }

    @Test
    fun `should list entries by user`() {
        val userId = UUID.randomUUID()
        val entries =
            listOf(
                FilmLibraryEntry(UUID.randomUUID(), userId, UUID.randomUUID(), null, false),
            )

        every { filmLibraryEntryRepository.findByUserId(userId) } returns entries

        assertEquals(entries, filmLibraryService.list(userId))

        verify(exactly = 1) { filmLibraryEntryRepository.findByUserId(userId) }
    }
}
