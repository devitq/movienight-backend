package com.project.movienight.adapters.persistence.jdbc

import com.project.movienight.domain.model.Film
import com.project.movienight.domain.model.FilmLibraryEntry
import com.project.movienight.domain.model.User
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
class FilmLibraryEntryRepositoryIntegrationTest {
    @Autowired
    private lateinit var filmLibraryEntryRepository: FilmLibraryEntryRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var filmRepository: FilmRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var testUser: User
    private lateinit var testFilm: Film

    @BeforeEach
    fun setup() {
        cleanDatabase()
        createTestData()
    }

    @AfterEach
    fun cleanup() {
        cleanDatabase()
    }

    private fun cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM favorites")
        jdbcTemplate.execute("DELETE FROM films")
        jdbcTemplate.execute("DELETE FROM users")
    }

    private fun createTestData() {
        testUser = User(UUID.randomUUID(), "Иван Иванов", "ivan@example.com", null)
        testFilm = Film(UUID.randomUUID(), "Начало", "Захватывающий триллер")
        userRepository.save(testUser)
        filmRepository.save(testFilm)
    }

    @Test
    fun `should save new film library entry and return saved entry`() {
        val entry =
            FilmLibraryEntry(
                id = UUID.randomUUID(),
                userId = testUser.id,
                filmId = testFilm.id,
                comment = "Отличный фильм!",
                isViewed = false,
            )

        val savedEntry = filmLibraryEntryRepository.save(entry)

        assertNotNull(savedEntry)
        assertEquals(entry.id, savedEntry.id)
        assertEquals(entry.userId, savedEntry.userId)
        assertEquals(entry.filmId, savedEntry.filmId)
        assertEquals(entry.comment, savedEntry.comment)
        assertEquals(entry.isViewed, savedEntry.isViewed)
    }

    @Test
    fun `should update existing film library entry`() {
        val entryId = UUID.randomUUID()
        val originalEntry = FilmLibraryEntry(entryId, testUser.id, testFilm.id, "Хочу посмотреть", false)
        filmLibraryEntryRepository.save(originalEntry)

        val updatedEntry = FilmLibraryEntry(entryId, testUser.id, testFilm.id, "Уже посмотрел, потрясающе!", true)
        val result = filmLibraryEntryRepository.save(updatedEntry)

        assertEquals(entryId, result.id)
        assertEquals("Уже посмотрел, потрясающе!", result.comment)
        assertTrue(result.isViewed)

        val foundEntry = filmLibraryEntryRepository.findById(entryId)
        assertNotNull(foundEntry)
        assertEquals("Уже посмотрел, потрясающе!", foundEntry?.comment)
        assertTrue(foundEntry?.isViewed ?: false)
    }

    @Test
    fun `should find film library entry by id`() {
        val entry = FilmLibraryEntry(UUID.randomUUID(), testUser.id, testFilm.id, "Обязательно посмотреть", false)
        filmLibraryEntryRepository.save(entry)

        val foundEntry = filmLibraryEntryRepository.findById(entry.id)

        assertNotNull(foundEntry)
        assertEquals(entry.id, foundEntry?.id)
        assertEquals(entry.userId, foundEntry?.userId)
        assertEquals(entry.filmId, foundEntry?.filmId)
        assertEquals(entry.comment, foundEntry?.comment)
        assertEquals(entry.isViewed, foundEntry?.isViewed)
    }

    @Test
    fun `should return null when film library entry not found by id`() {
        val nonExistentId = UUID.randomUUID()

        val foundEntry = filmLibraryEntryRepository.findById(nonExistentId)

        assertNull(foundEntry)
    }

    @Test
    fun `should find all film library entries`() {
        val entry1 = FilmLibraryEntry(UUID.randomUUID(), testUser.id, testFilm.id, "Комментарий 1", false)
        val entry2 = FilmLibraryEntry(UUID.randomUUID(), testUser.id, testFilm.id, "Комментарий 2", true)
        val entry3 = FilmLibraryEntry(UUID.randomUUID(), testUser.id, testFilm.id, null, false)

        filmLibraryEntryRepository.save(entry1)
        filmLibraryEntryRepository.save(entry2)
        filmLibraryEntryRepository.save(entry3)

        val allEntries = filmLibraryEntryRepository.findAll()

        assertEquals(3, allEntries.size)
        assertTrue(allEntries.any { it.id == entry1.id })
        assertTrue(allEntries.any { it.id == entry2.id })
        assertTrue(allEntries.any { it.id == entry3.id })
    }

    @Test
    fun `should return empty list when no film library entries exist`() {
        val allEntries = filmLibraryEntryRepository.findAll()

        assertTrue(allEntries.isEmpty())
    }

    @Test
    fun `should delete film library entry by id`() {
        val entry = FilmLibraryEntry(UUID.randomUUID(), testUser.id, testFilm.id, "Для удаления", false)
        filmLibraryEntryRepository.save(entry)

        filmLibraryEntryRepository.deleteById(entry.id)

        val foundEntry = filmLibraryEntryRepository.findById(entry.id)
        assertNull(foundEntry)
    }

    @Test
    fun `should not throw exception when deleting non-existent entry`() {
        val nonExistentId = UUID.randomUUID()

        filmLibraryEntryRepository.deleteById(nonExistentId)
    }

    @Test
    fun `should save entry with null comment`() {
        val entry = FilmLibraryEntry(UUID.randomUUID(), testUser.id, testFilm.id, null, false)

        val savedEntry = filmLibraryEntryRepository.save(entry)

        assertNotNull(savedEntry)
        assertNull(savedEntry.comment)
    }

    @Test
    fun `should save entry with isViewed true`() {
        val entry = FilmLibraryEntry(UUID.randomUUID(), testUser.id, testFilm.id, "Посмотрел", true)

        val savedEntry = filmLibraryEntryRepository.save(entry)

        assertNotNull(savedEntry)
        assertTrue(savedEntry.isViewed)
    }

    @Test
    fun `should save entry with isViewed false`() {
        val entry = FilmLibraryEntry(UUID.randomUUID(), testUser.id, testFilm.id, "Еще не смотрел", false)

        val savedEntry = filmLibraryEntryRepository.save(entry)

        assertNotNull(savedEntry)
        assertFalse(savedEntry.isViewed)
    }

    @Test
    fun `should cascade delete entries when user is deleted`() {
        val entry = FilmLibraryEntry(UUID.randomUUID(), testUser.id, testFilm.id, "Любимый фильм пользователя", false)
        filmLibraryEntryRepository.save(entry)

        userRepository.deleteById(testUser.id)

        val foundEntry = filmLibraryEntryRepository.findById(entry.id)
        assertNull(foundEntry)
    }

    @Test
    fun `should cascade delete entries when film is deleted`() {
        val entry = FilmLibraryEntry(UUID.randomUUID(), testUser.id, testFilm.id, "Запись о фильме", false)
        filmLibraryEntryRepository.save(entry)

        filmRepository.deleteById(testFilm.id)

        val foundEntry = filmLibraryEntryRepository.findById(entry.id)
        assertNull(foundEntry)
    }
}
