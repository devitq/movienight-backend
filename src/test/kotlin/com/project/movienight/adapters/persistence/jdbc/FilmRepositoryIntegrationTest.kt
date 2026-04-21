package com.project.movienight.adapters.persistence.jdbc

import com.project.movienight.domain.model.Film
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
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
class FilmRepositoryIntegrationTest {
    @Autowired
    private lateinit var filmRepository: FilmRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setup() {
        cleanDatabase()
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

    @Test
    fun `should save new film and return saved film`() {
        val film =
            Film(
                id = UUID.randomUUID(),
                title = "Начало",
                description = "Захватывающий триллер о снах внутри снов",
            )

        val savedFilm = filmRepository.save(film)

        assertNotNull(savedFilm)
        assertEquals(film.id, savedFilm.id)
        assertEquals(film.title, savedFilm.title)
        assertEquals(film.description, savedFilm.description)
    }

    @Test
    fun `should update existing film`() {
        val filmId = UUID.randomUUID()
        val originalFilm = Film(filmId, "Начало", "Оригинальное описание")
        filmRepository.save(originalFilm)

        val updatedFilm = Film(filmId, "Начало (Обновлено)", "Обновленное описание с дополнительными деталями")
        val result = filmRepository.save(updatedFilm)

        assertEquals(filmId, result.id)
        assertEquals("Начало (Обновлено)", result.title)
        assertEquals("Обновленное описание с дополнительными деталями", result.description)

        val foundFilm = filmRepository.findById(filmId)
        assertNotNull(foundFilm)
        assertEquals("Начало (Обновлено)", foundFilm?.title)
        assertEquals("Обновленное описание с дополнительными деталями", foundFilm?.description)
    }

    @Test
    fun `should find film by id`() {
        val film = Film(UUID.randomUUID(), "Матрица", "Хакер узнает правду о реальности")
        filmRepository.save(film)

        val foundFilm = filmRepository.findById(film.id)

        assertNotNull(foundFilm)
        assertEquals(film.id, foundFilm?.id)
        assertEquals(film.title, foundFilm?.title)
        assertEquals(film.description, foundFilm?.description)
    }

    @Test
    fun `should return null when film not found by id`() {
        val nonExistentId = UUID.randomUUID()

        val foundFilm = filmRepository.findById(nonExistentId)

        assertNull(foundFilm)
    }

    @Test
    fun `should find all films`() {
        val film1 = Film(UUID.randomUUID(), "Начало", "Сны внутри снов")
        val film2 = Film(UUID.randomUUID(), "Матрица", "Реальность не то, чем кажется")
        val film3 = Film(UUID.randomUUID(), "Интерстеллар", "Путешествие сквозь пространство и время")

        filmRepository.save(film1)
        filmRepository.save(film2)
        filmRepository.save(film3)

        val allFilms = filmRepository.findAll()

        assertEquals(3, allFilms.size)
        assertTrue(allFilms.any { it.id == film1.id })
        assertTrue(allFilms.any { it.id == film2.id })
        assertTrue(allFilms.any { it.id == film3.id })
    }

    @Test
    fun `should return empty list when no films exist`() {
        val allFilms = filmRepository.findAll()

        assertTrue(allFilms.isEmpty())
    }

    @Test
    fun `should delete film by id`() {
        val film = Film(UUID.randomUUID(), "Начало", "Сны внутри снов")
        filmRepository.save(film)

        filmRepository.deleteById(film.id)

        val foundFilm = filmRepository.findById(film.id)
        assertNull(foundFilm)
    }

    @Test
    fun `should not throw exception when deleting non-existent film`() {
        val nonExistentId = UUID.randomUUID()

        filmRepository.deleteById(nonExistentId)
    }

    @Test
    fun `should save film with long description`() {
        val longDescription = "А".repeat(1000)
        val film = Film(UUID.randomUUID(), "Тестовый фильм", longDescription)

        val savedFilm = filmRepository.save(film)

        assertNotNull(savedFilm)
        assertEquals(longDescription, savedFilm.description)
    }
}
