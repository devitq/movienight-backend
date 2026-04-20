package com.project.movienight.adapters.persistence.jdbc

import com.project.movienight.domain.model.Film
import com.project.movienight.domain.model.FilmLibrary
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
class FilmLibraryRepositoryIntegrationTest {
    @Autowired
    private lateinit var filmLibraryRepository: FilmLibraryRepository

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
        val entry = FilmLibrary(
            id = UUID.randomUUID(),
            userId = testUser.id,
            filmId = testFilm.id,
            comment = "Отличный фильм!",
            isViewed = false,
        )

        val savedEntry = filmLibraryRepository.save(entry)

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
        val originalEntry = FilmLibrary(entryId, testUser.id, testFilm.id, "Хочу посмотреть", false)
        filmLibraryRepository.save(originalEntry)

        val updatedEntry = FilmLibrary(entryId, testUser.id, testFilm.id, "Уже посмотрел, потрясающе!", true)
        val result = filmLibraryRepository.save(updatedEntry)

        assertEquals(entryId, result.id)
        assertEquals("Уже посмотрел, потрясающе!", result.comment)
        assertTrue(result.isViewed)

        val foundEntry = filmLibraryRepository.findById(entryId)
        assertNotNull(foundEntry)
        assertEquals("Уже посмотрел, потрясающе!", foundEntry?.comment)
        assertTrue(foundEntry?.isViewed ?: false)
    }

    @Test
    fun `should find film library entry by id`() {
        val entry = FilmLibrary(UUID.randomUUID(), testUser.id, testFilm.id, "Обязательно посмотреть", false)
        filmLibraryRepository.save(entry)

        val foundEntry = filmLibraryRepository.findById(entry.id)

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

        val foundEntry = filmLibraryRepository.findById(nonExistentId)

        assertNull(foundEntry)
    }

    @Test
    fun `should find all film library entries`() {
        val entry1 = FilmLibrary(UUID.randomUUID(), testUser.id, testFilm.id, "Комментарий 1", false)
        val entry2 = FilmLibrary(UUID.randomUUID(), testUser.id, testFilm.id, "Комментарий 2", true)
        val entry3 = FilmLibrary(UUID.randomUUID(), testUser.id, testFilm.id, null, false)

        filmLibraryRepository.save(entry1)
        filmLibraryRepository.save(entry2)
        filmLibraryRepository.save(entry3)

        val allEntries = filmLibraryRepository.findAll()

        assertEquals(3, allEntries.size)
        assertTrue(allEntries.any { it.id == entry1.id })
        assertTrue(allEntries.any { it.id == entry2.id })
        assertTrue(allEntries.any { it.id == entry3.id })
    }

    @Test
    fun `should return empty list when no film library entries exist`() {
        val allEntries = filmLibraryRepository.findAll()

        assertTrue(allEntries.isEmpty())
    }

    @Test
    fun `should delete film library entry by id`() {
        val entry = FilmLibrary(UUID.randomUUID(), testUser.id, testFilm.id, "Для удаления", false)
        filmLibraryRepository.save(entry)

        filmLibraryRepository.deleteById(entry.id)

        val foundEntry = filmLibraryRepository.findById(entry.id)
        assertNull(foundEntry)
    }

    @Test
    fun `should not throw exception when deleting non-existent entry`() {
        val nonExistentId = UUID.randomUUID()

        filmLibraryRepository.deleteById(nonExistentId)
    }

    @Test
    fun `should save entry with null comment`() {
        val entry = FilmLibrary(UUID.randomUUID(), testUser.id, testFilm.id, null, false)

        val savedEntry = filmLibraryRepository.save(entry)

        assertNotNull(savedEntry)
        assertNull(savedEntry.comment)
    }

    @Test
    fun `should save entry with isViewed true`() {
        val entry = FilmLibrary(UUID.randomUUID(), testUser.id, testFilm.id, "Посмотрел", true)

        val savedEntry = filmLibraryRepository.save(entry)

        assertNotNull(savedEntry)
        assertTrue(savedEntry.isViewed)
    }

    @Test
    fun `should save entry with isViewed false`() {
        val entry = FilmLibrary(UUID.randomUUID(), testUser.id, testFilm.id, "Еще не смотрел", false)

        val savedEntry = filmLibraryRepository.save(entry)

        assertNotNull(savedEntry)
        assertFalse(savedEntry.isViewed)
    }

    @Test
    fun `should cascade delete entries when user is deleted`() {
        val entry = FilmLibrary(UUID.randomUUID(), testUser.id, testFilm.id, "Любимый фильм пользователя", false)
        filmLibraryRepository.save(entry)

        userRepository.deleteById(testUser.id)

        val foundEntry = filmLibraryRepository.findById(entry.id)
        assertNull(foundEntry)
    }

    @Test
    fun `should cascade delete entries when film is deleted`() {
        val entry = FilmLibrary(UUID.randomUUID(), testUser.id, testFilm.id, "Запись о фильме", false)
        filmLibraryRepository.save(entry)

        filmRepository.deleteById(testFilm.id)

        val foundEntry = filmLibraryRepository.findById(entry.id)
        assertNull(foundEntry)
    }
}
