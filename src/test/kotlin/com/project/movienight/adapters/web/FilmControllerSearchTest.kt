package com.project.movienight.adapters.web

import com.project.movienight.application.ports.input.FilmUseCase
import com.project.movienight.domain.model.Film
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.UUID

class FilmControllerSearchTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var filmUseCase: FilmUseCase

    @BeforeEach
    fun setup() {
        filmUseCase = mockk()

        val controller =
            FilmController(
                filmUseCase = filmUseCase,
            )

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `search returns film when title exists`() {
        val title = "Inception"
        val film = Film(id = UUID.randomUUID(), title = title, description = "A dream heist")

        every { filmUseCase.searchByTitle(title) } returns film

        mockMvc
            .get("/api/films/search") {
                param("title", title)
            }.andExpect {
                status { isOk() }
                jsonPath("$.id") { value(film.id.toString()) }
                jsonPath("$.title") { value(title) }
                jsonPath("$.description") { value("A dream heist") }
            }

        verify(exactly = 1) { filmUseCase.searchByTitle(title) }
    }

    @Test
    fun `search returns 404 when title is not found`() {
        val title = "Unknown Title"

        every { filmUseCase.searchByTitle(title) } returns null

        mockMvc
            .get("/api/films/search") {
                param("title", title)
            }.andExpect {
                status { isNotFound() }
                content { string("") }
            }

        verify(exactly = 1) { filmUseCase.searchByTitle(title) }
    }
}
